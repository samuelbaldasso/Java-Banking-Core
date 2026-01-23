package com.sbaldasso.java_banking_core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JavaBankingCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(JavaBankingCoreApplication.class, args);
	}

}
