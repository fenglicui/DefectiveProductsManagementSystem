package cn.edu.iip.nju.web;

import cn.edu.iip.nju.dao.NewsDataDao;
import cn.edu.iip.nju.dao.WebDataDao;
import cn.edu.iip.nju.model.NewsData;
import cn.edu.iip.nju.model.vo.LableVO;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * created by xulei
 * 2019/5/5
 */
@Controller
public class LabelController {
    @Autowired
    NewsDataDao newsDataDao;

    @GetMapping("/label")
    public String showLabel(@ModelAttribute LableVO lableVO) { return "Label"; }

    @PostMapping("/label")
    public String getLabel(@ModelAttribute LableVO lableVO, @RequestParam String url , @RequestParam String pageUrl) {
        lableVO.setLabelurl(url);
        //更新数据库或者solr
        List<NewsData> news = newsDataDao.getAllByUrl(url);
        for (NewsData newsData : news) {
            if (!Strings.isNullOrEmpty(lableVO.getIsInjure())) {
                if("1".equals(lableVO.getIsInjure()))
                    newsData.setLabelInjure(1);
                else
                    newsData.setLabelInjure(0);
                //newsData.setLabelInjure("1".equals(lableVO.getIsInjure()));
            }
            if (!Strings.isNullOrEmpty(lableVO.getIsDupli()))
                newsData.setLabelDup(1);
            else
                newsData.setLabelDup(0);

                //newsData.setLabelDup("dup".equals(lableVO.getIsDupli()));
            if (!Strings.isNullOrEmpty(lableVO.getOtherLabel())) {
                newsData.setLabelOther(lableVO.getOtherLabel());
            }
            System.out.println(newsData.getLabelInjure());
            System.out.println(newsData.getLabelDup());
            System.out.println(newsData.getLabelOther());
        }
        newsDataDao.save(news);
        String param = pageUrl.split("\\?")[1];
        System.out.println(news);
        System.out.println(param);
        return "redirect:search"+"?"+param;
    }
}
