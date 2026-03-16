package com.vibe.sports;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SportsNewsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SportsNewsApplication.class, args);
    }
}

