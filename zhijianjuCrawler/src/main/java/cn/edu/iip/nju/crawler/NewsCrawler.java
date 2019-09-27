package cn.edu.iip.nju.crawler;

import cn.edu.hfut.dmic.contentextractor.ContentExtractor;
import cn.edu.hfut.dmic.contentextractor.News;
import cn.edu.iip.nju.constants.ICrawlerConstants;
import cn.edu.iip.nju.dao.NewsDataDao;
import cn.edu.iip.nju.model.NewsData;
import cn.edu.iip.nju.util.DateUtil;
import cn.edu.iip.nju.util.ReadFileUtil;
import com.google.common.collect.Sets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;


/**
 * 改造为多线程
 * Created by xu on 2017/11/22.
 */
@Service
public class NewsCrawler {
    private static final Logger logger = LoggerFactory.getLogger(NewsCrawler.class);
    private static volatile BloomFilter<String> bf = BloomFilter.create(Funnels.stringFunnel(Charset.forName("utf-8")),
            100000, 0.00001);

    private final NewsDataDao newsDataDao;

    private Set<String> keyWords = ReadFileUtil.readKeyWords();

    @Autowired
    private NewsCrawler(NewsDataDao newsDataDao) {
        this.newsDataDao = newsDataDao;
    }

    private Document getHtmlDoc(String url) throws IOException {
        return Jsoup.connect(url)
                //.userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
                .userAgent("Mozilla/5.0 (Windows NT 6.1; rv:30.0) Gecko/20100101 Firefox/30.0")
                .timeout(0)
                //.followRedirects(true)
                //.cookie("auth", "token")
                .get();
    }

    private void doSaveNews(Set<String> crawler) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (String s : crawler) {
            News news;
            try {
                news = ContentExtractor.getNewsByUrl(s);
                NewsData newsData = new NewsData();
                String title = news.getTitle();
                if (title.contains("快视频")) {
                    continue;
                }
                String url = news.getUrl();
                String content = news.getContent();

                newsData.setCrawlerTime(new Date());
                newsData.setTitle(title);
                newsData.setContent(content);
                newsData.setUrl(url);

                Document htmlDoc = getHtmlDoc(s);
                String htmlContent = htmlDoc.text();
                Date date = DateUtil.getDate(htmlContent);
//                if (date == null) {
//                    String time = news.getTime();
//                    Date dateFromParse = sdf.parse(time);
//                    Calendar current = Calendar.getInstance();
//                    Calendar calendar = Calendar.getInstance();
//                    calendar.setTime(dateFromParse);
//                    if (current.compareTo(calendar) >= 0) {
//                        date = dateFromParse;
//                    }
//                }
                //TODO date可能是NULL 需要人工修正
                newsData.setPostTime(date);
                //save
                newsDataDao.save(newsData);

                logger.info("saving news done");
            } catch (Exception e) {
                logger.error("save news error", e);
            }

        }
    }

    private Set<String> getUrlsFromBaidu(String keyWord) {
        Set<String> urlSet = Sets.newHashSet();
        StringBuilder realUrl = new StringBuilder(ICrawlerConstants.URL_BAIDU)
                .append(keyWord);

        try {
            Document htmlDoc = getHtmlDoc(realUrl.toString());
            //先确定页数 这个page的href里没有本页（第一页），所以后续要填上
            Element paginationElement = htmlDoc.select("p#page").first();
            if (paginationElement == null) {
                return urlSet;
            }
            List<Element> urlElements = paginationElement.select("a[href]");
            urlElements = urlElements.subList(0, 2);
            //为了添上第一页，这里转成URL
            LinkedList<String> urls = new LinkedList<>();
            urls.add(realUrl.toString());
            for (Element urlElement : urlElements) {
                urls.add(urlElement.attr("abs:href"));
            }
            for (String pageUrl : urls) {
                //取前三页，分别连接对应page
                Document eachPage = getHtmlDoc(pageUrl);
                Elements results = eachPage.select("div.result").select("h3.c-title");
                for (Element result : results) {
                    Element url = result.select("a[href]").first();
                    if (url == null) break;
                    String u = url.attr("abs:href");
                    synchronized (NewsCrawler.class) {
                        if (!bf.mightContain(u)) {
                            //bf中不存在
                            urlSet.add(u);
                            bf.put(u);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("爬虫超时", e);
        }
        return urlSet;
    }

    private Set<String> getUrlsFrom360(String keyWord) {
        Set<String> urlSet = Sets.newHashSet();
        for (int i = 1; i <= 2; i++) {
            StringBuilder realUrl = new StringBuilder(ICrawlerConstants.URL_360)
                    .append(keyWord)
                    .append(ICrawlerConstants.PAGE_NUM_360)
                    .append(i);
            try {
                Document htmlDoc = getHtmlDoc(realUrl.toString());
                Elements liElements = htmlDoc.select("li.res-list");
                for (Element liElement : liElements) {
                    Element aElement = liElement.select("h3 > a[href]").first();
                    String url = aElement.attr("abs:href");
                    synchronized (NewsCrawler.class) {
                        if (!bf.mightContain(url)) {
                            //bf中不存在
                            urlSet.add(url);
                            bf.put(url);
                        }
                    }

                }
            } catch (IOException e) {
                logger.error("爬虫超时?", e);
            }
        }
        return urlSet;
    }

    private Set<String> getUrlsFromSougou(String keyWord) {
        Set<String> set = Sets.newHashSet();
        try {
            String product = URLEncoder.encode(keyWord.split(" ")[0], "UTF-8");
            String injure = URLEncoder.encode(keyWord.split(" ")[1], "UTF-8");
            StringBuilder realUrl = new StringBuilder(ICrawlerConstants.URL_SOUGOU)
                    .append(product)
                    .append("%20")
                    .append(injure);
            Document htmlDoc = getHtmlDoc(realUrl.toString());
            Set<String> urls = processEachPageOfSougou(htmlDoc);

            set.addAll(urls);
            Elements pages = htmlDoc.select("div#pagebar_container").select("a[href]");
            List<Element> as;
            if (pages.size() > 2) {
                as = pages.subList(0, 2);
            } else {
                as = pages.subList(0, pages.size());
            }
            for (Element a : as) {
                Document doc = getHtmlDoc(a.attr("abs:href"));
                Set<String> uls = processEachPageOfSougou(doc);
                set.addAll(uls);
            }
        } catch (Exception e) {
            logger.error("sougou error");
            e.printStackTrace();
        }
        return set;
    }

    private Set<String> processEachPageOfSougou(Document document) {
        Set<String> set = Sets.newHashSet();
        Elements as = document.select("div.results").select("div.vrwrap").select("a[href]");
        //System.out.println(as.size());
        for (Element a : as) {
            String uu = a.attr("abs:href");
            synchronized (NewsCrawler.class) {
                if (!bf.mightContain(uu)) {
                    //bf中不存在
                    bf.put(uu);
                    if (!uu.contains("news.sogou")) {
                        set.add(uu);
                    }
                }
            }
        }
        return set;
    }

    private void saveNewsFromUrl(Function<String, Set<String>> func) {
        for (String keyWord : this.keyWords) {
            Set<String> urlSet = func.apply(keyWord);
            doSaveNews(urlSet);
        }
    }

    //百度新闻文件读取的关键字组合,参数为空
    public void saveNewsFromBaidu() {
        saveNewsFromUrl(this::getUrlsFromBaidu);
    }

    //搜狗新闻文件读取的关键字组合,参数为空
    public void saveNewsFromSougou() {
        saveNewsFromUrl(this::getUrlsFromSougou);
    }


    //360新闻文件读取的关键字组合,参数为空
    public void saveNewsFrom360() {
        saveNewsFromUrl(this::getUrlsFrom360);
    }

}
