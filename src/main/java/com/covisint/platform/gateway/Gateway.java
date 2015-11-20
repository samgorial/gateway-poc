package com.covisint.platform.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = { "com.covisint.platform.gateway" })
// @EnableAspectJAutoProxy
public class Gateway {

	static {
		System.loadLibrary("alljoyn_java");
	}

	public static void main(String[] args) {
		SpringApplication.run(Gateway.class, args);
	}

}
