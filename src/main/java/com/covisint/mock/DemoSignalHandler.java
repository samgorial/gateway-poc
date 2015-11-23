package com.covisint.mock;

import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;

import org.alljoyn.bus.annotation.BusSignalHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.covisint.platform.gateway.mqtt.MqttProducerService;

public class DemoSignalHandler {

	private static final Logger LOG = LoggerFactory.getLogger(DemoSignalHandler.class);

	public String deviceId;

	public Map<String, String> eventNames = new HashMap<>();

	public MqttProducerService eventPublisher;

	public DemoSignalHandler(String deviceId, Map<String, String> eventNames, MqttProducerService eventPublisher) {
		this.deviceId = deviceId;
		this.eventNames = eventNames;
		this.eventPublisher = eventPublisher;
	}

	@BusSignalHandler(iface = "com.covisint.platform.device.demo.DemoInterface", signal = "internalTempChanged")
	public void internalTempChanged(double temp) throws Exception {
		LOG.info("Remote Pi: internal temperature changed to {}", temp);
		JsonObject args = Json.createObjectBuilder().add("newTemp", temp).build();
		eventPublisher.publishMessage(deviceId, eventNames.get("internalTempChanged"), args);
	}

	@BusSignalHandler(iface = "com.covisint.platform.device.demo.DemoInterface", signal = "ledColorChanged")
	public void ledColorChanged(String newColor) throws Exception {
		LOG.info("Remote Pi: LED color changed to {}", newColor);
		JsonObject args = Json.createObjectBuilder().add("newColor", newColor).build();
		eventPublisher.publishMessage(deviceId, eventNames.get("ledColorChanged"), args);
	}

}