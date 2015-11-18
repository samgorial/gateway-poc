package com.covisint.platform.gateway.util;

import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.JsonValue.ValueType;

import org.alljoyn.bus.SessionOpts;

public class AllJoynSupport {

	public static final SessionOpts getDefaultSessionOpts() {
		SessionOpts opts = new SessionOpts();
		opts.traffic = SessionOpts.TRAFFIC_MESSAGES;
		opts.isMultipoint = false;
		opts.proximity = SessionOpts.PROXIMITY_ANY;
		opts.transports = SessionOpts.TRANSPORT_IP;
		return opts;
	}

	public static JsonObject validateCommand(JsonStructure input) {

		if (input == null) {
			throw new RuntimeException("JSON was null or empty.");
		}

		if (input.getValueType() != ValueType.OBJECT) {
			throw new RuntimeException("Expected JSON object but was " + input.getValueType());
		}

		JsonObject jsonObject = (JsonObject) input;

		checkJsonPropertyExists(jsonObject, "messageId");
		checkJsonPropertyExists(jsonObject, "deviceId");
		checkJsonPropertyExists(jsonObject, "commandTemplateId");

		return jsonObject;
	}

	public static void checkJsonPropertyExists(JsonObject json, String propertyName) {
		if (!json.containsKey(propertyName)) {
			throw new RuntimeException("Missing property " + propertyName);
		}

		if (json.get(propertyName).getValueType() != ValueType.STRING) {
			throw new RuntimeException("Expected property " + propertyName + " to be a string");
		}

		String value = json.getString(propertyName).trim();

		if (value == null || value.length() == 0) {
			throw new RuntimeException(propertyName + " was empty.");
		}
	}

}
