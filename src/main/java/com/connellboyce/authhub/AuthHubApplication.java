package com.connellboyce.authhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableMethodSecurity
public class AuthHubApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthHubApplication.class, args);
	}

}
