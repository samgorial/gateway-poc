package com.covisint.platform.gateway;

import static com.covisint.platform.gateway.util.AllJoynSupport.validateCommand;

import java.io.StringReader;
import java.util.Date;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonStructure;

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
	public void sendCommand(@RequestBody String input) {

		LOG.debug("Processing command: \n{}", input);

		// FIXME accept JSON directly instead of parsing strings.
		JsonReader reader = Json.createReader(new StringReader(input));

		JsonStructure json = reader.read();

		JsonObject command = validateCommand(json);

		delegate.process(command);

		LOG.debug("Successfully processed command.");
	}

}