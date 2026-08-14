package io.github.vitaa1.vencefacil;

import org.springframework.boot.SpringApplication;

public class TestVenceFacilApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(VenceFacilApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
