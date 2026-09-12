package com.campusagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 校园综合服务平台启动类。
 */
@SpringBootApplication
@MapperScan("com.campusagent.mapper")
public class CampusAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusAgentApplication.class, args);
    }
}
