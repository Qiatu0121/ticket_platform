package com.ticket.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling        // 开启定时任务（超时关单用）
@MapperScan("com.ticket.platform.mapper")
@SpringBootApplication
public class TicketPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketPlatformApplication.class, args);
    }
}
