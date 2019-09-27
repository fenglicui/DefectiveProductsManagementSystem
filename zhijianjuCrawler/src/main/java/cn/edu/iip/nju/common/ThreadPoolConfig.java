package cn.edu.iip.nju.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@Configuration
public class ThreadPoolConfig {
    @Bean
    public ExecutorService getThreadPool(){
        return Executors.newFixedThreadPool(4);
    }

    @Bean(name = "hosBlockingQueue")
    public BlockingQueue<File> getBlockingQueue(){
        return new LinkedBlockingQueue<>();
    }
}
