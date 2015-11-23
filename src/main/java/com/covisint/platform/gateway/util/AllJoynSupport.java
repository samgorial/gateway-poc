package com.covisint.platform.gateway.util;

import java.util.List;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.alljoyn.bus.SessionOpts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.covisint.platform.device.core.DataType;
import com.covisint.platform.device.core.commandtemplate.CommandTemplate;
import com.covisint.platform.device.core.eventtemplate.EventTemplate;
import com.covisint.platform.gateway.discovery.DefaultProvisionerService.IsInputArg;
import com.covisint.platform.gateway.discovery.DefaultProvisionerService.IsOutputArg;
import com.covisint.platform.gateway.domain.AJAnnotation;
import com.covisint.platform.gateway.domain.AJArg;
import com.covisint.platform.gateway.domain.AJMethod;
import com.covisint.platform.gateway.domain.AJSignal;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

public class AllJoynSupport {

	private static final Logger LOG = LoggerFactory.getLogger(AllJoynSupport.class);

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

	public static Object getJsonValueFromType(String argName, String ajType, JsonValue json) {

		switch (ajType) {
		case "s":
		case "y":
			if (json.getValueType() != ValueType.STRING) {
				throw new IllegalArgumentException(
						"Expected string value for arg " + argName + " but got " + json.getValueType());
			}
			return ((JsonString) json).getString();
		case "b":
			if (json.getValueType() != ValueType.FALSE && json.getValueType() != ValueType.TRUE) {
				throw new IllegalArgumentException(
						"Expected boolean value for arg " + argName + " but got " + json.getValueType());
			}
			return json == JsonValue.TRUE ? true : false;
		case "d":
			if (json.getValueType() != ValueType.NUMBER) {
				throw new IllegalArgumentException(
						"Expected number (decimal) value for arg " + argName + " but got " + json.getValueType());
			}
			return ((JsonNumber) json).doubleValue();
		case "n":
		case "q":
		case "i":
		case "u":
		case "x":
		case "t":
			if (json.getValueType() != ValueType.NUMBER) {
				throw new IllegalArgumentException(
						"Expected number (integer) value for arg " + argName + " but got " + json.getValueType());
			}
			return ((JsonNumber) json).intValue();
		default:
			throw new IllegalArgumentException("Unsuppored AJ type id " + ajType);
		}

	}

	public static CommandTemplate getCommandTemplateForMethod(AJMethod method, List<CommandTemplate> commandTemplates) {
		Optional<CommandTemplate> optional = FluentIterable.from(commandTemplates)
				.firstMatch(new Predicate<CommandTemplate>() {

					public boolean apply(CommandTemplate input) {
						if (input == null) {
							return false;
						}
						return input.getName().equals(method.getName());
					}

				});

		if (!optional.isPresent()) {
			return null;
		}

		return optional.get();
	}

	public static EventTemplate getEventTemplateForSignal(AJSignal signal, List<EventTemplate> eventTemplates) {
		Optional<EventTemplate> optional = FluentIterable.from(eventTemplates)
				.firstMatch(new Predicate<EventTemplate>() {

					public boolean apply(EventTemplate input) {
						if (input == null) {
							return false;
						}
						return input.getName().equals(signal.getName());
					}

				});

		if (!optional.isPresent()) {
			return null;
		}

		return optional.get();
	}

	public static boolean matchCommandArgs(CommandTemplate commandTemplate, AJMethod method,
			CommandArgMatchProcessor processor) {

		if (method.getArgs() == null) {
			// No args, just assume a match.
			return true;
		}

		// Count number of method arguments.
		List<AJArg> methodArgs = FluentIterable.from(method.getArgs()).filter(IsInputArg.INSTANCE).toList();

		// Compare to number of command template arguments.
		if (commandTemplate.getArgs().size() != methodArgs.size()) {
			LOG.warn("OOOPS!  Command template had {} args but AJ method had {} (inbound).  Skipping command template.",
					commandTemplate.getArgs().size(), methodArgs.size());
			return false;
		}

		int idx = 0;
		for (AJArg arg : methodArgs) {

			DataType methodArgType = AllJoynSupport.getDataType(arg.getType());
			DataType commandArgType = commandTemplate.getArgs().get(idx).getDataType();

			if (methodArgType != commandArgType) {
				LOG.warn("Shoot, method and command arg types differ at index {}: {} vs {}", idx, methodArgType,
						commandArgType);
				return false;
			}

			String methodArgName = "arg" + idx;
			String commandArgName = commandTemplate.getArgs().get(idx).getName();

			if (method.getAnnotations() != null) {
				for (AJAnnotation annotation : method.getAnnotations()) {
					if (methodArgName.equalsIgnoreCase(annotation.getName())) {
						methodArgName = annotation.getValue();
					}
				}
			}

			if (!methodArgName.equalsIgnoreCase(commandArgName)) {
				LOG.warn("Oh so close!  Method and command arg names differ at index {}: {} vs {}", idx, methodArgName,
						commandArgName);
				return false;
			}

			if (processor != null) {
				processor.onMatch(methodArgName, commandArgName, arg.getType(), commandArgType);
			}

			idx++;
		}

		return true;

	}

	public static boolean matchEventFields(EventTemplate eventTemplate, AJSignal signal,
			EventFieldMatchProcessor processor) {

		if (signal.getArgs() == null) {
			// No args, just assume a match.
			return true;
		}

		// Count number of signal arguments.
		List<AJArg> signalArgs = FluentIterable.from(signal.getArgs()).filter(IsOutputArg.INSTANCE).toList();

		// Compare to number of command template arguments.
		if (eventTemplate.getEventFields().size() != signalArgs.size()) {
			LOG.warn("OOOPS!  Event template had {} fields but AJ method had {} (outbound).  Skipping event template.",
					eventTemplate.getEventFields().size(), signalArgs.size());
			return false;
		}

		int idx = 0;
		for (AJArg arg : signalArgs) {

			DataType signalArgType = AllJoynSupport.getDataType(arg.getType());
			DataType eventFieldType = eventTemplate.getEventFields().get(idx).getDataType();

			if (signalArgType != eventFieldType) {
				LOG.warn("Shoot, signal and event arg types differ at index {}: {} vs {}", idx, signalArgType,
						eventFieldType);
				return false;
			}

			String signalArgName = "arg" + idx;
			String eventFieldName = eventTemplate.getEventFields().get(idx).getName();

			if (signal.getAnnotations() != null) {
				for (AJAnnotation annotation : signal.getAnnotations()) {
					if (signalArgName.equalsIgnoreCase(annotation.getName())) {
						signalArgName = annotation.getValue();
					}
				}
			}

			if (!signalArgName.equalsIgnoreCase(eventFieldName)) {
				LOG.warn("Oh so close!  Signal and event arg names differ at index {}: {} vs {}", idx, signalArgName,
						eventFieldName);
				return false;
			}

			if (processor != null) {
				processor.onMatch(signalArgName, eventFieldName, arg.getType(), eventFieldType);
			}

			idx++;
		}

		return true;

	}

}
