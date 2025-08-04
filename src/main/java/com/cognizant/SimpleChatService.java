package com.cognizant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class SimpleChatService {
    public static void main(String[] args) {

        SpringApplication.run(SimpleChatService.class, args);
        log.info("Spring AI LangFuse Demo Application started successfully!");
    }

}
