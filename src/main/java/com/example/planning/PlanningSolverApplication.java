package com.example.planning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"fr.project.planning"})
public class PlanningSolverApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlanningSolverApplication.class, args);
	}

}
