package com.covisint.platform.gateway;

import java.util.Date;

import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.JsonValue.ValueType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.covisint.platform.gateway.bind.CommandDelegate;

@RestController
public class GatewayHttpService {

	private static final Logger LOG = LoggerFactory.getLogger(GatewayHttpService.class);

	@Autowired
	private CommandDelegate delegate;

	@RequestMapping(value = "/ping")
	public String ping() {
		return new Date().toString();
	}

	@RequestMapping(value = "/command", method = RequestMethod.POST)
	public void sendCommand(@RequestBody JsonStructure input) {
		LOG.debug("Processing command: {}", input);

		JsonObject command = validate(input);

		delegate.process(command);

		LOG.debug("Successfully processed command.");
	}

	private JsonObject validate(JsonStructure input) {

		if (input == null) {
			throw new RuntimeException("JSON was null or empty.");
		}

		if (input.getValueType() != ValueType.OBJECT) {
			throw new RuntimeException("Expected JSON object but was " + input.getValueType());
		}

		// TODO finish.

		return (JsonObject) input;
	}

}