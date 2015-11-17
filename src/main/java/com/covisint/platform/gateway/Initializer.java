package com.covisint.platform.gateway;

import javax.annotation.PostConstruct;

import org.alljoyn.bus.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.covisint.platform.gateway.session.DefaultAboutListener;
import com.covisint.platform.gateway.session.DefaultBusListener;

import mock.PiSignalHandler;

@Component
public class Initializer {

	@Value("${advertised.name.pfx}")
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

		bus.getBusAttachment().registerBusListener(signalListener);
		bus.getBusAttachment().registerSignalHandlers(new PiSignalHandler());

		Status status = bus.getBusAttachment().findAdvertisedName(advertisedNamePrefix);

		if (status != Status.OK) {
			throw new ExceptionInInitializerError(status.toString());
		}
		
	}

}
