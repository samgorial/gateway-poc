package com.covisint.platform.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableAspectJAutoProxy
public class Gateway {

	static {
		System.loadLibrary("alljoyn_java");
	}

	public static void main(String[] args) {
		SpringApplication.run(Gateway.class, args);
	}

}
