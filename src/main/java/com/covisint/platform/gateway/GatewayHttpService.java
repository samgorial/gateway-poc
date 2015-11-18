package com.covisint.platform.gateway;

import static com.covisint.platform.gateway.util.AllJoynSupport.validateCommand;

import java.util.Date;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ModelAttribute;
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
	public void sendCommand(@ModelAttribute("jsonEntityBody") @RequestBody JsonStructure json) {

		LOG.debug("Processing command: \n{}", json);

		JsonObject command = validateCommand(json);

		delegate.process(command);

		LOG.debug("Successfully processed command.");
	}

	@ModelAttribute("jsonEntityBody")
	private JsonStructure getJsonBody(HttpServletRequest request) {
		try (final JsonReader jsonReader = Json.createReader(request.getInputStream())) {
			return jsonReader.read();
		} catch (Exception e) {
			throw new RuntimeException("Could not read or parse JSON entity body.", e);
		}
	}

}