package com.sns.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.sns")
@EnableJpaRepositories(basePackages = "com.sns")
@EntityScan(basePackages = "com.sns")
@EnableScheduling
@EnableAsync
public class SnsApplication {
    public static void main(String[] args) {
        SpringApplication.run(SnsApplication.class, args);
    }
}
