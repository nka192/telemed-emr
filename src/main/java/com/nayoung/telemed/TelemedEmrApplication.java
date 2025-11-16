package com.nayoung.telemed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TelemedEmrApplication {

	public static void main(String[] args) {
		SpringApplication.run(TelemedEmrApplication.class, args);
	}

}
