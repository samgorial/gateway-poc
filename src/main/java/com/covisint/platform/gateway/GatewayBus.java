package com.covisint.platform.gateway;

import java.util.Arrays;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.alljoyn.bus.AboutListener;
import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.covisint.mock.SignalHandler;
import com.covisint.platform.gateway.repository.session.SessionRepository;

@Component
public class GatewayBus {

	private static final String AJ_DEFAULT_BUS_ADDRESS = "org.alljoyn.bus.address";

	private static final Logger LOG = LoggerFactory.getLogger(GatewayBus.class);

	@Value("${alljoyn.watched_interfaces}")
	private String[] interfaces;

	@Autowired
	private AboutListener aboutListener;

	@Autowired
	private BusAttachment attachment;

	@Autowired
	private SessionRepository sessionRepository;

	@PostConstruct
	public void init() {
		sessionRepository.clearAll();
	}

	@PreDestroy
	public void shutdown() {
		attachment.disconnect();
		LOG.info("Disconnected the from the bus.");
	}

	@Bean
	private BusAttachment createBusAttachment(@Value("${agent.name}") String applicationName) {

		BusAttachment bus = new BusAttachment(applicationName, BusAttachment.RemoteMessage.Receive);

		Status status = bus.connect();

		if (status != Status.OK) {
			LOG.error("Could not create bus attachment; connection failed: {}", status);
			throw new RuntimeException("Could not create bus attachment: " + status.toString());
		}

		LOG.info("Created bus attachment on {}", System.getProperty(AJ_DEFAULT_BUS_ADDRESS));

		// Register the About announcement listener.
		bus.registerAboutListener(aboutListener);
		
		// TODO can't do it like this
		bus.registerSignalHandlers(SignalHandler.INSTANCE);

		LOG.info("Target interfaces passed to who-implements call: {}", Arrays.deepToString(interfaces));
		
		status = bus.whoImplements(interfaces);

		if (status != Status.OK) {
			LOG.error("Could not call who-implements method on about bus attachment: {}", status);
			throw new RuntimeException("Could not call who-implements method: " + status.toString());
		}

		return bus;
	}

	public BusAttachment getBusAttachment() {
		return attachment;
	}

}
