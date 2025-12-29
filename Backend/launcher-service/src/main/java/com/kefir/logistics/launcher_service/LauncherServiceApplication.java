package com.kefir.logistics.launcher_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LauncherServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LauncherServiceApplication.class, args);
    }
}