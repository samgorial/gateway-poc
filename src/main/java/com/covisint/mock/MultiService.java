package com.covisint.mock;

import org.alljoyn.bus.BusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiService implements PingInterface, SayHelloInterface {

	private static final Logger LOG = LoggerFactory.getLogger(MultiService.class);

	public void ping(String message) throws BusException {
		LOG.info(message);
	}

	public void hello(String name) throws BusException {
		LOG.info("Hello, {}", name);
	}

}