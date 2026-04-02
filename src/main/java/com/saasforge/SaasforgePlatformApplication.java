package com.saasforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableAsync
public class SaasforgePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaasforgePlatformApplication.class, args);
    }
}
