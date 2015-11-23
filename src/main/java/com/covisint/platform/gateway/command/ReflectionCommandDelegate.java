package com.covisint.platform.gateway.command;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.alljoyn.bus.ProxyBusObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.covisint.platform.gateway.GatewayBus;
import com.covisint.platform.gateway.repository.catalog.ArgMapping;
import com.covisint.platform.gateway.repository.catalog.CatalogItem;
import com.covisint.platform.gateway.repository.catalog.CatalogRepository;
import com.covisint.platform.gateway.repository.catalog.MethodMapping;
import com.covisint.platform.gateway.repository.session.AboutSession;
import com.covisint.platform.gateway.repository.session.SessionEndpoint;
import com.covisint.platform.gateway.repository.session.SessionRepository;
import com.covisint.platform.gateway.util.AllJoynSupport;
import com.google.api.client.util.Base64;

@Component
public class ReflectionCommandDelegate implements CommandDelegate {

	private static final Logger LOG = LoggerFactory.getLogger(ReflectionCommandDelegate.class);

	@Autowired
	private GatewayBus bus;

	@Autowired
	private SessionRepository sessionRepository;

	@Autowired
	private CatalogRepository catalogRepository;

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

		// FIXME shouldn't occur. Loop through all sessions?
		SessionEndpoint endpoint = endpoints.get(0);

		AboutSession session = endpoint.getParentSession();

		String busName = session.getBusName();
		int sessionId = session.getSessionId();

		LOG.debug("Session id {} found for device {}", sessionId, deviceId);

		CatalogItem catalogItem = catalogRepository.searchByInterface(endpoint.getIntf());

		if (catalogItem == null) {
			throw new IllegalStateException("Interface " + endpoint.getIntf() + " not cataloged.");
		}

		Class<?> interfaceClass;

		try {
			interfaceClass = Class.forName(endpoint.getIntf());
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("No interface class found on path: " + endpoint.getIntf());
		}

		LOG.debug("Resolved class for interface {}", endpoint.getIntf());

		ProxyBusObject proxyBusObject = bus.getBusAttachment().getProxyBusObject(busName, endpoint.getPath(), sessionId,
				new Class<?>[] { interfaceClass });

		LOG.debug("Retrieved proxy bus object for bus {} and target interface.", busName);

		Object remote = proxyBusObject.getInterface(interfaceClass);

		if (remote == null) {
			throw new IllegalStateException("No remote object for " + interfaceClass.getName());
		}

		LOG.debug("Retrieved remote object from proxy.");

		MethodMapping methodMapping = getMethodMapping(commandTemplateId, catalogItem);

		String methodName = methodMapping.getMethodName();

		LOG.debug("Found mapped method for command template id {}.  Searching for Java method name: ",
				commandTemplateId, methodName);

		Method targetMethod = null;

		for (Method method : interfaceClass.getMethods()) {

			if (methodName.equals(method.getName())) {
				targetMethod = method;
				break;
			}

		}

		if (targetMethod == null) {
			throw new IllegalStateException(
					"No method " + methodName + " was found on interface " + interfaceClass.getName());
		}

		LOG.debug("Found target Java method for {}", methodName);

		List<ArgMapping> argMappings = methodMapping.getArgs();

		Object[] args;

		if (argMappings == null) {
			args = new Object[0];
			LOG.debug("No arguments declared for method {}", methodName);
		} else {
			LOG.debug("Setting up {} arguments for method {}", argMappings.size(), methodName);
			args = new Object[argMappings.size()];
			int i = 0;
			for (ArgMapping argMapping : argMappings) {
				String commandArgName = argMapping.getCommandArgName();
				String argType = argMapping.getArgType();
				JsonValue jsonValue = commandArgs.get(commandArgName);

				if (jsonValue == null) {
					throw new IllegalArgumentException("Argument " + commandArgName + " was not provided.");
				}

				Object value = AllJoynSupport.getJsonValueFromType(commandArgName, argType, jsonValue);

				args[i] = value;

				LOG.debug("Arg {} is of type {} and has value {}", i, value.getClass().getName(), value);

				i++;

			}

		}

		LOG.debug("Set up all arguments.  Now invoking target method on remote object.");

		try {
			targetMethod.invoke(remote, args);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException("Error occurred while invoking remote method " + methodName, e);
		}

		LOG.debug("Successfully invoked remote method.");

		// try {
		//
		// switch (commandTemplateId) {
		// case "ping":
		// Class<?> someItfc = null;
		// try {
		// someItfc = Class.forName("blah");
		// } catch (ClassNotFoundException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// Object o = getProxy(deviceId, busName, endpoint.getPath(),
		// sessionId).getInterface(someItfc);
		//
		// getProxy(deviceId, busName, endpoint.getPath(),
		// sessionId).getInterface(PingInterface.class)
		// .ping(new Date().toString());
		// break;
		// case "hello":
		// if (!commandArgs.containsKey("name")) {
		// throw new IllegalArgumentException("Expected parameter 'name'");
		// }
		// getProxy(deviceId, busName, endpoint.getPath(),
		// sessionId).getInterface(SayHelloInterface.class)
		// .hello(commandArgs.getString("name"));
		// break;
		// case "turn_on_buzzer":
		// getProxy(deviceId, busName, endpoint.getPath(),
		// sessionId).getInterface(DemoInterface.class)
		// .turnOnBuzzer();
		// break;
		// case "turn_off_buzzer":
		// getProxy(deviceId, busName, endpoint.getPath(),
		// sessionId).getInterface(DemoInterface.class)
		// .turnOffBuzzer();
		// break;
		// default:
		// throw new UnsupportedOperationException(commandTemplateId);
		// }
		//
		// } catch (BusException e) {
		// throw new RuntimeException(e);
		// }

	}

	private MethodMapping getMethodMapping(String commandTemplateId, CatalogItem catalogItem) {

		for (MethodMapping mapping : catalogItem.getMethodMappings()) {
			if (commandTemplateId.equals(mapping.getCommandTemplateId())) {
				return mapping;
			}
		}

		throw new IllegalArgumentException("Unknown operation specified by command template id " + commandTemplateId
				+ ". No MethodMapping found.");
	}

	// private ProxyBusObject getProxy(String deviceId, String busName, String
	// path, int sessionId) {
	//
	// switch (deviceId) {
	// case "c963ba8bae3a":
	// return bus.getBusAttachment().getProxyBusObject(busName, path, sessionId,
	// new Class<?>[] { DemoInterface.class });
	// default:
	// return bus.getBusAttachment().getProxyBusObject(busName, path, sessionId,
	// new Class<?>[] { PiBusInterface.class, PingInterface.class,
	// SayHelloInterface.class });
	// }
	//
	// }

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
