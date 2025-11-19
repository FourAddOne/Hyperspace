package com.lihuahua.hyperspace;

import org.slf4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.lihuahua.hyperspace"})
public class HyperspaceApplication {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(HyperspaceApplication.class);

    public static void main(String[] args) {

        SpringApplication.run(HyperspaceApplication.class, args);
        //清楚控制台
        System.out.print("\033[H\033[2J");
        System.out.flush();
        logger.info("=======超时空专列已启动=======");

    }

}