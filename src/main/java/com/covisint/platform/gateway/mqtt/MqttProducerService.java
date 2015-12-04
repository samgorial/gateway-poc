package com.covisint.platform.gateway.mqtt;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.google.api.client.util.Base64;
import com.google.common.base.Stopwatch;

@Component
public class MqttProducerService extends BaseMqttService implements MqttCallback {

	private static final Logger LOG = LoggerFactory.getLogger(MqttProducerService.class);

	@Autowired
	private MqttClient mqttClient;

	public void connectionLost(Throwable t) {
		LOG.debug("Connection lost: {}", t.getMessage(), t);
	}

	public void deliveryComplete(IMqttDeliveryToken token) {
	}

	public void messageArrived(String topicName, MqttMessage message) throws Exception {
	}

	@Bean
	public MqttClient mqttClient() {
		MqttConnectOptions connOpt = new MqttConnectOptions();

		connOpt.setCleanSession(true);
		connOpt.setKeepAliveInterval(30);
		connOpt.setUserName(username);
		connOpt.setPassword(password.toCharArray());

		MqttClient client;

		try {
			client = new MqttClient(url, clientId + "-" + qualifier, clientPersistence());
//			client = new MqttClient(url, clientId + "-" + qualifier);
			client.setCallback(this);
			client.connect(connOpt);
		} catch (MqttException e) {
			throw new ExceptionInInitializerError(e);
		}

		LOG.debug("Connected to broker {} with client id {}", url, client.getClientId());

		return client;
	}

	public void publishMessage(String deviceId, String eventTemplateId, JsonObject args) throws Exception {

		Stopwatch watch = Stopwatch.createStarted();

		MqttTopic topic = mqttClient.getTopic(producerTopic);

		JsonObjectBuilder payload = Json.createObjectBuilder();

		payload.add("messageId", UUID.randomUUID().toString());
		payload.add("deviceId", deviceId);
		payload.add("eventTemplateId", eventTemplateId);
		payload.add("message", Base64.encodeBase64String(args.toString().getBytes()));
		payload.add("encodingType", "base64");

		String payloadString = payload.build().toString();

		LOG.debug("Payload being sent: {}", payloadString);

		MqttMessage message = new MqttMessage(payloadString.getBytes());
		message.setQos(defaultQos);
		message.setRetained(false);

		MqttDeliveryToken token = topic.publish(message);

		token.waitForCompletion();

		LOG.debug("Published message for event template {} and device {} in {}", eventTemplateId, deviceId, watch);
	}
}