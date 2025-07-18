package com.rinhabackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RinhaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(RinhaBackendApplication.class, args);
		System.out.println("Hello World!");
	}

}
