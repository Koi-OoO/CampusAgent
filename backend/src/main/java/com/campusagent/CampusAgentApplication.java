package com.campusagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 校园综合服务平台启动类。
 */
@SpringBootApplication
@MapperScan("com.campusagent.mapper")
@EnableScheduling
public class CampusAgentApplication {

    /**
     * 启动校园综合服务平台。
     *
     * @param args 应用启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(CampusAgentApplication.class, args);
    }
}
