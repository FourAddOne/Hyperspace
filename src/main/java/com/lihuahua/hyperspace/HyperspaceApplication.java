package com.lihuahua.hyperspace;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.lihuahua.hyperspace.mapper")
@EnableScheduling // 启用定时任务
public class HyperspaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HyperspaceApplication.class, args);
    }

}