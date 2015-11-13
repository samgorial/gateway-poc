package com.covisint.platform.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Gateway {

    static {
        System.loadLibrary("alljoyn_java");
    }
    
	public static void main(String[] args) {
		SpringApplication.run(Gateway.class, args);
	}

}
