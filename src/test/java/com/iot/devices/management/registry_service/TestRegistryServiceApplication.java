package com.iot.devices.management.registry_service;

import org.springframework.boot.SpringApplication;

public class TestRegistryServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(RegistryServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
