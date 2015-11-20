package com.covisint.mock;

import org.alljoyn.bus.BusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingleService implements SayHelloInterface {

	private static final Logger LOG = LoggerFactory.getLogger(SingleService.class);

	public String getId() {
		return "id-12345";
	}

	public void hello(String name) throws BusException {
		LOG.info("Hello, {}", name);
	}

	public String statusChanged() throws BusException {
		return System.currentTimeMillis() + "";
	}

}