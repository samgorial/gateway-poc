package com.covisint.platform.gateway.bind;

import java.io.ByteArrayInputStream;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.ProxyBusObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.covisint.platform.gateway.GatewayBus;
import com.covisint.platform.gateway.repository.SessionEndpoint;
import com.covisint.platform.gateway.repository.SessionRepository;
import com.google.api.client.util.Base64;

import mock.PiBusInterface;

@Component
public class HardcodedCommandDelegate implements CommandDelegate {

	private static final Logger LOG = LoggerFactory.getLogger(HardcodedCommandDelegate.class);

	@Autowired
	private GatewayBus bus;

	@Autowired
	private SessionRepository sessionRepository;

	public void process(JsonObject command) {

		String messageId = command.getString("messageId");
		String deviceId = command.getString("deviceId");
		String commandTemplateId = command.getString("commandTemplateId");
		String message = command.getString("message");

		JsonReader reader = Json.createReader(new ByteArrayInputStream(Base64.decodeBase64(message)));

		JsonObject commandArgs = reader.readObject();

		LOG.debug("About to process command for messageId[{}], deviceId[{}], commandTemplateId[{}]", messageId,
				deviceId, commandTemplateId);

		LOG.debug("Command args: {}", commandArgs);

		// SessionInfo session = sessionRepository.getDeviceSession(deviceId);

		List<SessionEndpoint> endpoints = sessionRepository.getEndpointsByDevice(deviceId);

		if (endpoints == null || endpoints.isEmpty()) {
			throw new IllegalStateException("Did not find any session endpoints for device " + deviceId);
		}

		if (endpoints.size() > 1) {
			LOG.warn("Found {} session endpoints for device {} but only expected 1.", endpoints.size(), deviceId);
		}

		SessionEndpoint endpoint = endpoints.get(0);

		// LOG.debug("Session id {} found for device {}",
		// session.getSessionId(), deviceId);

		ProxyBusObject proxy = bus.getBusAttachment().getProxyBusObject(endpoint.getIntf(), endpoint.getPath(),
				endpoint.getParentSession().getSessionId(), new Class<?>[] { PiBusInterface.class });

		PiBusInterface service = proxy.getInterface(PiBusInterface.class);

		try {

			switch (commandTemplateId) {
			case "ping":
				service.ping("" + System.currentTimeMillis());
				break;
			case "turn_on_buzzer":
				service.turnOnBuzzer();
				break;
			case "turn_off_buzzer":
				service.turnOffBuzzer();
				break;
			default:
				throw new UnsupportedOperationException(commandTemplateId);
			}

		} catch (BusException e) {
			throw new RuntimeException(e);
		}

	}

}
