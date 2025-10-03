package com.krag.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.krag")
public class KragApplication {
    public static void main(String[] args) {
        SpringApplication.run(KragApplication.class, args);
    }
}