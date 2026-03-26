package com.saasforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class SaasforgePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaasforgePlatformApplication.class, args);
    }
}
