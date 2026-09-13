package com.github.rickysurya.tenderofferapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TenderOfferApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TenderOfferApiApplication.class, args);
	}

}
