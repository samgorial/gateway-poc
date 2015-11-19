package com.covisint.platform.gateway.util;

import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.JsonValue.ValueType;

import org.alljoyn.bus.SessionOpts;

import com.covisint.platform.device.core.DataType;

public class AllJoynSupport {

	public static final SessionOpts getDefaultSessionOpts() {
		SessionOpts opts = new SessionOpts();
		opts.traffic = SessionOpts.TRAFFIC_MESSAGES;
		opts.isMultipoint = false;
		opts.proximity = SessionOpts.PROXIMITY_ANY;
		opts.transports = SessionOpts.TRANSPORT_ANY;
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

	public static DataType getDataType(String ajType) {

		switch (ajType) {
		case "s":
		case "y":
			return DataType.STRING;
		case "b":
			return DataType.BOOL;
		case "n":
		case "q":
		case "i":
		case "u":
		case "x":
		case "t":
			return DataType.INTEGER;
		case "d":
			return DataType.DECIMAL;
		}

		return null;
	}
}
