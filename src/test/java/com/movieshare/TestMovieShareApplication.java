package com.movieshare;

import org.springframework.boot.SpringApplication;

public class TestMovieShareApplication {

	public static void main(String[] args) {
		SpringApplication.from(MovieShareApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
