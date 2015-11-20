package com.covisint.mock;

import org.alljoyn.bus.BusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiService extends SingleService implements PingInterface {

	private static final Logger LOG = LoggerFactory.getLogger(MultiService.class);

	public void ping(String message) throws BusException {
		LOG.info(message);
	}

}