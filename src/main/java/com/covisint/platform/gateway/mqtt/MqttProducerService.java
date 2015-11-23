package com.covisint.platform.gateway.mqtt;

import java.util.UUID;

import javax.annotation.PreDestroy;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.codec.binary.Base64;
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

import com.google.common.base.Stopwatch;

@Component
public class MqttProducerService extends BaseMqttService implements MqttCallback {

	private static final Logger LOG = LoggerFactory.getLogger(MqttProducerService.class);

	@Autowired
	private MqttClient client;

	@PreDestroy
	public void shutdown() {
		if (client != null) {
			try {
				client.disconnect();
				client.close();
			} catch (MqttException e) {
				LOG.error("Error occurred while closing client.", e);
			}
		}
	}

	public void connectionLost(Throwable t) {
		LOG.error("Connection lost.", t);
	}

	public void deliveryComplete(IMqttDeliveryToken token) {
		LOG.debug("Delivery complete: {}", token.toString());
	}

	public void messageArrived(String topicName, MqttMessage message) throws Exception {
		LOG.debug("Message arrived for topic {}: {}", topicName, new String(message.getPayload()));
	}

	public void publishMessage(String deviceId, String eventTemplateId, JsonObject args) throws Exception {

		Stopwatch watch = Stopwatch.createStarted();

		MqttTopic topic = client.getTopic(producerTopic);

		JsonObjectBuilder payload = Json.createObjectBuilder();

		payload.add("messageId", UUID.randomUUID().toString());
		payload.add("deviceId", deviceId);
		payload.add("eventTemplateId", eventTemplateId);
		payload.add("message", Base64.encodeBase64String(args.toString().getBytes()));

		MqttMessage message = new MqttMessage(payload.build().toString().getBytes());
		message.setQos(defaultQos);
		message.setRetained(false);

		MqttDeliveryToken token = topic.publish(message);

		token.waitForCompletion();

		LOG.debug("Published message for event template {} and device {} in {}", eventTemplateId, deviceId, watch);
	}

	@Bean
	private MqttClient getMqttClient() {
		try {
			MqttClient c = new MqttClient(url, clientId);

			c.setCallback(this);
			
			MqttConnectOptions connectOpts = new MqttConnectOptions();

			connectOpts.setCleanSession(true);
			connectOpts.setKeepAliveInterval(30);
			connectOpts.setUserName(username);
			connectOpts.setPassword(password.toCharArray());

			c.connect(connectOpts);

			LOG.debug("Connected to MQTT broker at {} with client id {}", url, clientId);

			return c;
		} catch (MqttException e) {
			throw new RuntimeException(e);
		}
	}
}