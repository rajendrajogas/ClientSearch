package com.client.search.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ClientSearchApplication {

	private static Logger LOG = LoggerFactory.getLogger(ClientSearchApplication.class);

	public static void main(String[] args) {
		LOG.info("Launching the application");
		SpringApplication.run(ClientSearchApplication.class, args);
	}

}
