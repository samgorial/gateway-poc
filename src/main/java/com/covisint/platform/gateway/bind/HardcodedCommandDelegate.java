package com.covisint.platform.gateway.bind;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.ProxyBusObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.covisint.mock.PiBusInterface;
import com.covisint.mock.PingInterface;
import com.covisint.mock.SayHelloInterface;
import com.covisint.platform.device.demo.DemoInterface;
import com.covisint.platform.gateway.GatewayBus;
import com.covisint.platform.gateway.repository.session.AboutSession;
import com.covisint.platform.gateway.repository.session.SessionEndpoint;
import com.covisint.platform.gateway.repository.session.SessionRepository;
import com.google.api.client.util.Base64;

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

		JsonObject commandArgs = readArgs(command);

		LOG.debug("About to process command for messageId[{}], deviceId[{}], commandTemplateId[{}]", messageId,
				deviceId, commandTemplateId);

		List<SessionEndpoint> endpoints = sessionRepository.getEndpointsByDevice(deviceId);

		if (endpoints == null || endpoints.isEmpty()) {
			throw new IllegalStateException("Did not find any session endpoints for device " + deviceId);
		}

		if (endpoints.size() > 1) {
			LOG.warn("Found {} session endpoints for device {} but only expected 1.", endpoints.size(), deviceId);
		}

		// FIXME not this way
		SessionEndpoint endpoint = endpoints.get(0);

		AboutSession session = endpoint.getParentSession();
		String busName = session.getBusName();
		int sessionId = session.getSessionId();

		LOG.debug("Session id {} found for device {}", sessionId, deviceId);

		try {

			switch (commandTemplateId) {
			case "ping":
				getProxy(deviceId, busName, endpoint.getPath(), sessionId).getInterface(PingInterface.class)
						.ping(new Date().toString());
				break;
			case "hello":
				if (!commandArgs.containsKey("name")) {
					throw new IllegalArgumentException("Expected parameter 'name'");
				}
				getProxy(deviceId, busName, endpoint.getPath(), sessionId).getInterface(SayHelloInterface.class)
						.hello(commandArgs.getString("name"));
				break;
			case "turn_on_buzzer":
				getProxy(deviceId, busName, endpoint.getPath(), sessionId).getInterface(DemoInterface.class)
						.turnOnBuzzer();
				break;
			case "turn_off_buzzer":
				getProxy(deviceId, busName, endpoint.getPath(), sessionId).getInterface(DemoInterface.class)
						.turnOffBuzzer();
				break;
			default:
				throw new UnsupportedOperationException(commandTemplateId);
			}

		} catch (BusException e) {
			throw new RuntimeException(e);
		}

	}

	private ProxyBusObject getProxy(String deviceId, String busName, String path, int sessionId) {

		switch (deviceId) {
		case "c963ba8bae3a":
			return bus.getBusAttachment().getProxyBusObject(busName, path, sessionId,
					new Class<?>[] { DemoInterface.class });
		default:
			return bus.getBusAttachment().getProxyBusObject(busName, path, sessionId,
					new Class<?>[] { PiBusInterface.class, PingInterface.class, SayHelloInterface.class });
		}

	}

	private JsonObject readArgs(JsonObject command) {

		JsonString jsonString = command.getJsonString("message");

		if (jsonString == null) {
			return Json.createObjectBuilder().build();
		}

		String message = jsonString.getString();
		JsonReader reader = Json.createReader(new ByteArrayInputStream(Base64.decodeBase64(message)));
		JsonObject commandArgs = reader.readObject();

		LOG.debug("Command args: {}", commandArgs);

		return commandArgs;
	}

}
