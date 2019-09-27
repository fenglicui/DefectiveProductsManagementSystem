package cn.edu.iip.nju;

import cn.edu.iip.nju.crawler.*;
import cn.edu.iip.nju.crawler.fujian.ExcelProcess;
import cn.edu.iip.nju.service.NewsDataService;
import cn.edu.iip.nju.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

@SpringBootApplication
public class DefectiveProductionCrawlerApplication implements CommandLineRunner {
    private static Logger logger = LoggerFactory.getLogger(DefectiveProductionCrawlerApplication.class);
    @Autowired
    ExecutorService pool;
    //京东
    @Autowired
    JingDongCrawler jingDongCrawler;
    //质检网站
    @Autowired
    ZhaoHui zhaoHui;//ok 3000tiao
    @Autowired
    SuwangZhijian suwangZhijian;//ok
    @Autowired
    Xiaoxie xiaoxie;//ok 2743条
    @Autowired
    JiangSu jiangSu;//ok 有下载
    @Autowired
    GJZLJDJYJYZJ gjzljdjyjyzj;//ok 有下载
    @Autowired
    BJXFZXH bjxfzxh;
    @Autowired
    LNXFZXH lnxfzxh;

    @Autowired
    Zhiliangxinwenwang zhiliangxinwenwang;
    @Autowired
    Zhilianganquan zhilianganquan;

    //附件处理
    @Autowired
    ExcelProcess excelProcess;//ok

    //医院数据
    @Autowired
    HospotalDataService hospotalDataService;//ok

    //新闻网站
    @Autowired
    NewsCrawler newsCrawler;

    @Autowired
    Chinanews chinanews;

    @Autowired
    Fenghuang fenghuang;

    @Autowired
    Suzhou suzhou;


    @Autowired
    RedisService redisService;

    @Autowired
    NewsDataService newsDataService;
    @Autowired
    @Qualifier("hosBlockingQueue")
    BlockingQueue<File> blockingQueue;

    //    @Autowired
//    CPZLJDS cpzljds;//问题很大，网页变了，而且附件也没了
    @Override
    public void run(String... strings) throws Exception {
        newsCrawler.saveNewsFromBaidu();
        logger.info("finish");
    }

    public static void main(String[] args) {
        SpringApplication.run(DefectiveProductionCrawlerApplication.class, args);
    }
}
