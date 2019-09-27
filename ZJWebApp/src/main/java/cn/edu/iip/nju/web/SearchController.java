package cn.edu.iip.nju.web;

import cn.edu.iip.nju.model.Document;
import cn.edu.iip.nju.model.User;
import cn.edu.iip.nju.model.UserSearchHistory;
import cn.edu.iip.nju.model.vo.LableVO;
import cn.edu.iip.nju.service.SolrDocumentService;
import cn.edu.iip.nju.service.UserSearchHistoryService;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Date;
import java.util.List;
import java.sql.ResultSet;
import java.sql.SQLException;
import cn.edu.iip.nju.model.NewsData;
import java.sql.*;

/**
 * Created by xu on 2017/9/5.
 */
@Controller
@RequestMapping("/search")
@Slf4j
public class SearchController {
    @Autowired
    private UserSearchHistoryService userSearchHistoryService;
    @Autowired
    private SolrDocumentService solrDocumentService;

    @GetMapping
    public String processQuery(@RequestParam(name = "queryWord", required = false) String queryWord, Model model,
                               @RequestParam(name = "page", defaultValue = "0") Integer page,
                               @RequestParam(name = "sort", defaultValue = "keyWord") String sort,
                               @ModelAttribute LableVO lableVO) {
        //空查询则返回首页
        if (Strings.isNullOrEmpty(queryWord)) {
            return "forward:/index";
        }
        //log.info("queryword is %s",queryWord);
        //判断用户是否登录，如果是登录状态，则保存搜索历史
        Integer userId = isUserLogIn();
        if (userId != -1) {

            UserSearchHistory history = new UserSearchHistory();
            history.setUserId(userId);
            history.setSearchHistory(queryWord);
            history.setSearchTime(new Date());
            if (userSearchHistoryService.isNewSearchHistory(history)) {
                userSearchHistoryService.saveSearchHistory(history);
            }
        }
        Sort s;
        Page<Document> results;
        if (sort.equals("posttime")) {
            s = new Sort(Sort.Direction.DESC, "post_time");
            results = solrDocumentService.findBySearchText(queryWord, new PageRequest(page, 25, s));
        } else {
            results = solrDocumentService.findBySearchText(queryWord, new PageRequest(page, 25));
        }
        if (queryWord.contains("公司") || queryWord.contains("企业")) {
            List<Document> content = results.getContent();
            boolean isRelative = false;
            for (Document document : content) {
                if ((document.getTitle() + document.getContent()).contains(queryWord)) {
                    isRelative = true;
                }
            }
            if (!isRelative) {
                return "s/noresult";
            }
        }
        //results 里面每个document都有url 根据url查到对应的newsdata，拿到3个标签的值,并判断是否打过标签，为打过的默认是-1
        //并根据是否打过标签设置document中最后一个属性
        //todo 此处document里面属性还是空的，要从newsdata中根据url查到记录然后设置一下就有值了
 //           JdbcTemplate jdbcTemplate = new JdbcTemplate();
 //           List<NewsData> news = jdbcTemplate.query(select_sql, new MyRowMapper(), url);
  //          NewsData News = news.get(1);
  //          System.out.println(News.getlabelDup());
/*
        String URL="jdbc:mysql://114.212.80.2/zhijianju1?useSSL=true&autoReconnect=true&characterEncoding=UTF-8&seUnicode=true";
        //1.加载驱动程序
        int label_injure = -1;
        int label_dup = -1;
        String label_other = null;
        try{
            Class.forName("com.mysql.jdbc.Driver");
            System.out.println("成功加载MySQL驱动！");
            //2.获得数据库的连接
            Connection con = DriverManager.getConnection(URL, "root", "iipconfig");
            //3.通过数据库的连接操作数据库，实现增删改查

            List<Document> content = results.getContent();
            for (Document document : content) {
                String url = document.getUrl();
                //System.out.println(url);
                String select_sql = "select * from news_data where url='" + url + "'";
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery(select_sql);//选择import java.sql.ResultSet;
                System.out.println(select_sql);

                while (rs.next()) {//如果对象中有数据，就会循环打印出来
                    int ID = rs.getInt("id");
                    String content1 = rs.getString("content");
                    String url1 = rs.getString("url");
                    label_injure = rs.getInt("label_injure");
                    label_dup = rs.getInt("label_dup");
                    label_other = rs.getString("label_other");

                    System.out.println(label_injure);
                    System.out.println(label_dup);
                    System.out.println(label_other);

                    System.out.println(ID);
                    System.out.println(content1);
                    System.out.println(url1);
                }

                document.setLabelDup(label_dup);
                document.setLabelInjure(label_injure);
                document.setLabelOther(label_other);
                if (label_dup != -1 || label_injure != -1 || label_other != null)
                    document.setIsTagged(true);
                System.out.println(document.getIsTagged());
                rs.close();
            }
            con.close();
        }catch(SQLException e){
            System.out.println("Error"+e);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
*/
        model.addAttribute("queryWord", queryWord);
        model.addAttribute("results", results);
        model.addAttribute("sort", sort);
        model.addAttribute("lableVO", lableVO);
        return "s/result";

    }

    private Integer isUserLogIn() {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return currentUser == null ? -1 : currentUser.getId();
    }
}
