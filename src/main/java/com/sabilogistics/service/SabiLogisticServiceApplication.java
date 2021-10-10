package com.sabilogistics.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = "com.sabi")
@EntityScan(basePackages = {"com.sabilogisticscore.models"})
@SpringBootApplication
public class SabiLogisticServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SabiLogisticServiceApplication.class, args);
	}

}
