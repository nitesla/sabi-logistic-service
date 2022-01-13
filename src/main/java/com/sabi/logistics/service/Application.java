package com.sabi.logistics.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.Transactional;

@ComponentScan(basePackages = "com.sabi.framework")
@EntityScan(basePackages = {"com.sabi.logistics.core.models"})
@SpringBootApplication
@Transactional
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
