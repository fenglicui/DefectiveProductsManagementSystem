package cn.edu.iip.nju.service;

import cn.edu.iip.nju.dao.InjureCaseDao;
import cn.edu.iip.nju.model.InjureCase;
import cn.edu.iip.nju.model.NewsData;
import cn.edu.iip.nju.util.CityProvince;
import cn.edu.iip.nju.util.InjureLevelUtil;
import cn.edu.iip.nju.util.ReadFileUtil;
import cn.edu.iip.nju.util.WordLizeUtil.WordFilter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.StandardTokenizer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Created by xu on 2017/10/24.
 * 根据爬取的新闻建立伤害事件案例
 */
@Service
public class InjureCaseService {
    @Autowired
    private InjureCaseDao injureCaseDao;
    @Autowired
    private NewsDataService newsDataService;
    private static Logger logger = LoggerFactory.getLogger(InjureCaseService.class);


    /**
     * @param s:需要匹配的字符串
     * @param keywords:关键字集合
     * @return
     */
    private Set<String> searchKeyWord(String s, Set<String> keywords) {
        WordFilter wordFilter = new WordFilter();
        wordFilter.init(keywords);
        wordFilter.doFilter(s);
        return wordFilter.getKeyword();
    }


    /**
     * 提取伤害事件，首先根据标题判断是否有产品名称，如果标题中有的话直接就提取
     * 如果标题中没有，则需要在正文中找，但是正文中可能找到很多个，可以采用统计数量的方式来减少到3个以内
     * 其他字段就直接在正文中关键字碰撞
     *
     * @param newsData
     */
    private void processNews(NewsData newsData) throws IOException {
        InjureCase injureCase = new InjureCase();
        String title = newsData.getTitle();
        String content = title + newsData.getContent();


        Set<String> products = ReadFileUtil.readProducts();
        if (Strings.isNullOrEmpty(title)) {
            title = " ";
        }
        Set<String> pro = searchKeyWord(title, products);
        if (pro != null && pro.size() > 0) {
            String product = joinToString(pro);
            injureCase.setProductName(product);
        } else {
            Set<String> strings = searchKeyWord(content, products);
            //取出现频次最高的三个
            List<String> proSets = frequentWord(strings, content);
            injureCase.setProductName(joinToString(proSets));

        }//产品名ok
        Set<String> cities = ReadFileUtil.readCities();
        Set<String> cs = searchKeyWord(content, cities);
        List<String> city = frequentWord(cs, content);
        if (city != null && city.size() > 0) {
            injureCase.setInjureArea(city.get(0));
            String province = CityProvince.chooseProvinceOfCompany(city.get(0));
            injureCase.setProvince(province);//发生地点和省份ok
        }


        Set<String> injureType = ReadFileUtil.readInjureType();
        Set<String> ins = searchKeyWord(content, injureType);
        List<String> injureTypeSet = frequentWord(ins, content);
        injureCase.setInjureType(joinToString(injureTypeSet));//伤害类型


        String degree = InjureLevelUtil.checkInjureLevel(joinToString(injureTypeSet));
        injureCase.setInjureDegree(degree);
        Date date = newsData.getPostTime();
        if (date == null) {
            date = new Date();
        }
        //设置时分秒为0
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date  newdate = cal.getTime();
        injureCase.setInjureTime(newdate);
        System.out.println(newdate);

        injureCase.setUrl(newsData.getUrl());//url是唯一性索引，所以插入时遇到相同的url会抛异常，可以捕获

        try {
            injureCaseDao.save(injureCase);
            System.out.println(injureCase);
            logger.info("save injureCase success!");
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            logger.info("skip same url data");
        }
    }

    /**
     * 返回出现次数比较高的词
     *
     * @param words    要判断的词
     * @param sentence 出现次数前三的词
     * @return
     */
    private List<String> frequentWord(Set<String> words, String sentence) {
        List<String> result = Lists.newArrayList();
        if (words == null || words.size() == 0) return result;
        if (words.size() <= 3) {
            result.addAll(words);
            return result;
        }
        Map<String, Integer> map = Maps.newHashMap();
        for (String word : words) {
            map.put(word, 0);
        }

        List<Term> terms = StandardTokenizer.segment(sentence);
        for (Term term : terms) {
            String word = term.word;
            if (words.contains(word)) {
                map.put(word, map.get(word) + 1);
            }
        }
        ArrayList<Map.Entry<String, Integer>> list = Lists.newArrayList(map.entrySet());
        list.sort(Comparator.comparing(Map.Entry::getValue));
        Collections.reverse(list);
        result.add(list.get(0).getKey());
        result.add(list.get(1).getKey());
        result.add(list.get(2).getKey());
        return result;
    }


    private String joinToString(Collection<String> collection) {
        if (collection == null || collection.size() == 0) return "";
        if (collection.size() > 20) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String s : collection) {
            sb.append(s);
            sb.append(" ");
        }
        return sb.toString().trim();
    }

    public void saveInjureCase() {
        long pageNum = newsDataService.pageNum();
        for (long i = 0; i < pageNum; i++) {
            List<NewsData> newsDataList = newsDataService.getByPageForProcess((int) i);
            for (NewsData newsData : newsDataList) {
                try {
                    processNews(newsData);
                } catch (IOException e) {
                    logger.error("save injureCase fail", e);
                }
            }
        }
    }

    public void removeErrorPostTime() {
        List<InjureCase> injureCaseList = injureCaseDao.findAllByTime();
        System.out.println(injureCaseList.size());
        //injureCaseDao.save(injureCaseList);
    }


    @Transactional
    public void readFromCsv() throws IOException, ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        List<InjureCase> useful = Lists.newArrayList();
        Resource resource = new FileSystemResource("C:\\Users\\yajima\\Desktop\\1024_1.csv");
        File file = resource.getFile();
        List<String> lines = Files.readLines(file, Charset.forName("utf-8"));
        for (String line : lines) {
            String[] split = line.split(",");
            String productName = split[2];
            String injureType = split[3];
            String injureArea = split[4];
            String date = split[5];
            String url = split[6];
            if (split.length != 7) {
                continue;
            }
            InjureCase injureCase = injureCaseDao.findByUrl(url);
            if("\"\"".equals(injureArea)){
                injureArea = "";
            }
            if("\"\"".equals(injureType)){
                injureType = "";
            }
            injureCase.setInjureArea(injureArea);
            injureCase.setInjureTime(sdf.parse(date));
            injureCase.setProductName(productName);
            injureCase.setInjureType(injureType);
            useful.add(injureCase);
        }
        List<Integer> allIds = injureCaseDao.findAll().stream().map(InjureCase::getId).collect(Collectors.toList());
        List<Integer> useFul = useful.stream().map(InjureCase::getId).collect(Collectors.toList());
        allIds.removeAll(useFul);
        injureCaseDao.deleteAllByIdIn(allIds);
        injureCaseDao.save(useful);
    }

    @Transactional()
    public void modifyManually() throws IOException {
        Resource resource = new ClassPathResource("keywords/injureCase.csv");
        File file = resource.getFile();
        List<String> lines = Files.readLines(file, Charset.forName("utf-8"));
        for (String line : lines) {
            String[] words = line.split(",");
            if (words.length != 6) {
                System.out.println(words[0]);
            }
            Integer id = Integer.valueOf(words[0]);
            System.out.println(id);
            InjureCase injureCase = injureCaseDao.findOne(id);
            System.out.println(injureCase.getId());
            injureCase.setInjureArea(words[2].replaceAll("\"", ""));
            injureCase.setInjureType(words[3].replaceAll("\"", ""));
            injureCase.setProductName(words[1].replaceAll("\"", ""));
            injureCase.setUrl(words[5]);
            injureCaseDao.save(injureCase);


        }
        logger.info("modify success");
        Resource resource1 = new ClassPathResource("keywords/ids.txt");
        File file1 = resource1.getFile();
        lines = Files.readLines(file1, Charset.forName("utf-8"));
        Set<Integer> ids = Sets.newHashSet();
        for (String line : lines) {
            Integer id = Integer.valueOf(line.trim());
            ids.add(id);
        }
        List<Integer> idList = new ArrayList<>(ids);
        injureCaseDao.deleteAllByIdIn(idList);
        logger.info("finish");
    }

    public void test() {
        InjureCase injureCase = injureCaseDao.findOne(1);
        System.out.println(injureCase);
        injureCase.setInjureDegree("2");
        injureCaseDao.save(injureCase);
    }

    public void removeNoPro() {

        List<InjureCase> pros = injureCaseDao.getAllByProductNameLike("%香水%");
        List<InjureCase> pros1 = injureCaseDao.getAllByProductNameLike("%灭火器%");
        pros.addAll(pros1);
        for (InjureCase pro : pros) {
            String productName = pro.getProductName().trim();
            String[] split = productName.split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (String s : split) {
                if (!"香水".equals(s.trim()) && !"灭火器".equals(s.trim())) {
                    sb.append(s).append(" ");
                }
            }
            String proNameNEW = sb.toString().trim();
            pro.setProductName(proNameNEW);

        }
        injureCaseDao.save(pros);

    }


    private void removeBy(String keyWord, String type) {
        List<InjureCase> cases = new ArrayList<>();
        if ("productName".equals(keyWord)) {
            cases = injureCaseDao.getAllByProductNameLike("%" + type + "%");
        } else if ("injureType".equals(keyWord)) {
            cases = injureCaseDao.getAllByInjureTypeLike("%" + type + "%");
        }
        for (InjureCase aCase : cases) {
            String injureType = aCase.getInjureType();
            String[] split = injureType.split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (String s : split) {
                if (!type.equals(s.trim())) {
                    sb.append(s).append(" ");
                }
            }
            if (sb.length() == 0) {
                sb.append("其他");
            } else {
                sb.deleteCharAt(sb.length() - 1);
            }
            if ("productName".equals(keyWord)) {
                aCase.setProductName(sb.toString());
            } else if ("injureType".equals(keyWord)) {
                aCase.setInjureType(sb.toString());
            }
            System.out.println(sb);
        }
        injureCaseDao.save(cases);
    }

    public void removeType() {
        String[] type = {"抢救", "呼吸道", "噪音", "急救", "截肢", "损伤", "神经", "住院", "疲劳", "脱落", "浓烟"};
        for (String s : type) {
            removeBy("injureType", s);
            logger.info("finish remove {}", s);
        }

    }

    public void removeProductName() {
        String[] name = {"毛巾", "栏杆", "洗发水","香水",};
        for (String s : name) {
            removeBy("productName", s);
            logger.info("finish remove {}", s);
        }

    }

    public void mergePro(){
        Map<String,String> map = new HashMap<>();
        map.put("冰箱","电冰箱");
        map.put("炉灶","炉具");
        map.put("电瓶车","电动车");
        map.put("电磁灶","电磁炉");
        map.put("电饭煲","电饭锅");
        map.put("电视","电视机");
        map.put("风扇","电风扇");
        map.put("吹风机","电吹风");
        map.put("煤气瓶","煤气罐");
        map.put("熨斗","电熨斗");
        map.put("电取暖","取暖器");
        map.put("小太阳","取暖器");
        map.put("暖气","取暖器");
        map.put("电暖器","取暖器");
        map.put("灶台","燃气灶");

        for (Map.Entry<String, String> entry : map.entrySet()) {
            List<InjureCase> ins = injureCaseDao.findAllByProductNameLike("%" + entry.getKey() + "%");
            for (InjureCase in : ins) {
                String productName = in.getProductName();
                String[] s = productName.split(" ");
                Set<String> set = Sets.newHashSet();
                for (String s1 : s) {
                    if(map.containsKey(s1.trim())){
                        set.add(map.get(s1.trim()));
                    }else{
                        set.add(s1.trim());
                    }
                }
                String proNew = joinToString(set);
                in.setProductName(proNew);
                logger.info("injureCase is {}",in);
            }
            //injureCaseDao.save(ins);
        }

    }


    @Test
    public void test1(){
        String str = "冰箱 自行车 炉灶";
        String replace = str.replace("炉灶", "炉具");
        System.out.println(replace);
    }
}
