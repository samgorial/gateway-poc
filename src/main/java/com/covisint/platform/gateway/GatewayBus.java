package com.covisint.platform.gateway;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
public class GatewayBus {

	private static final Logger LOG = LoggerFactory.getLogger(GatewayBus.class);

	@Value("${watched.interfaces}")
	private String[] interfaces;

	@Resource
	private BusAttachment centralBusAttachment;

	@Resource
	private BusAttachment aboutBusAttachment;

	@PostConstruct
	public void init() {
		startListening(aboutBusAttachment, "about");
		startListening(centralBusAttachment, "central");
	}

	@PreDestroy
	public void shutdown() {
		centralBusAttachment.disconnect();
		aboutBusAttachment.disconnect();
	}

	private void startListening(BusAttachment attachment, String busType) {

		LOG.info("Starting to listen for implemented interfaces {} on {} bus.", interfaces, busType);

		Status status = attachment.whoImplements(interfaces);

		if (status != Status.OK) {
			LOG.error("Could not call who-implements method on about bus attachment: {}", status);
			throw new RuntimeException("Could not call who-implements method: " + status.toString());
		}
	}

	@Bean
	@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	private BusAttachment createBusAttachment(@Value("${agent.name}") String applicationName) {

		BusAttachment bus = new BusAttachment(applicationName, BusAttachment.RemoteMessage.Receive);

		Status status = bus.connect();

		if (status != Status.OK) {
			LOG.error("Could not create bus attachment; connection failed: {}", status);
			throw new RuntimeException("Could not create bus attachment: " + status.toString());
		}

		LOG.info("Created bus attachment on {}", System.getProperty("org.alljoyn.bus.address"));

		return bus;
	}

	public BusAttachment getAboutBusAttachment() {
		return aboutBusAttachment;
	}

	public BusAttachment getCentralBusAttachment() {
		return centralBusAttachment;
	}

}
