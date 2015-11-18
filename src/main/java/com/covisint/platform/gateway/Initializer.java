package com.covisint.platform.gateway;

import javax.annotation.PostConstruct;

import org.alljoyn.bus.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.covisint.platform.gateway.session.DefaultAboutListener;
import com.covisint.platform.gateway.session.DefaultBusListener;

import mock.PiSignalHandler;

@Component
public class Initializer {

	private static final Logger LOG = LoggerFactory.getLogger(Initializer.class);

	@Value("${alljoyn.advertised_name_pfx}")
	private String advertisedNamePrefix;

	@Autowired
	private GatewayBus bus;

	@Autowired
	private DefaultAboutListener aboutListener;

	@Autowired
	private DefaultBusListener signalListener;

	@PostConstruct
	public void init() {
		bus.getBusAttachment().registerAboutListener(aboutListener);

		try {
			// FIXME fix load order
			Thread.sleep(100);
		} catch (InterruptedException e) {
			LOG.error("Thread interrupted while sleeping.", e);
		}

		bus.getBusAttachment().registerBusListener(signalListener);
		bus.getBusAttachment().registerSignalHandlers(new PiSignalHandler());

		Status status = bus.getBusAttachment().findAdvertisedName(advertisedNamePrefix);

		if (status != Status.OK) {
			throw new ExceptionInInitializerError(status.toString());
		}

	}

}
