package com.movieshare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MovieShareApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(MovieShareApplication.class);
		app.setDefaultProperties(DotEnvLoader.loadAsDefaultProperties());
		app.run(args);
	}

}
