package io.github.vitaa1.gestorvp;

import org.springframework.boot.SpringApplication;

public class TestGestorVpApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(GestorVpApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
