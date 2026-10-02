package com.campusagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * CampusAgent 后端应用启动类。
 */
@SpringBootApplication
@MapperScan("com.campusagent.mapper")
@EnableScheduling
public class CampusAgentApplication {

    /**
     * 启动 Spring Boot 应用。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(CampusAgentApplication.class, args);
    }
}
