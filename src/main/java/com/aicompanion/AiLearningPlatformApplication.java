package com.aicompanion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AiLearningPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiLearningPlatformApplication.class, args);
    }
}
