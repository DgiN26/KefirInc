package com.example.TransactionSaga;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TransactionSagaApplication {

	public static void main(String[] args) {
		SpringApplication.run(TransactionSagaApplication.class, args);
	}

}