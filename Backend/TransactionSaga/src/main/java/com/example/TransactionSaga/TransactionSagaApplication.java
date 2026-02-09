package com.example.TransactionSaga;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TransactionSagaApplication {
    public static void main(String[] args) {
        SpringApplication.run(TransactionSagaApplication.class, args);
    }
}