package com.covisint.platform.gateway.mqtt;

import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Component;

@Component
public class MqttProducerService extends BaseMqttService {

	@Bean
	public MqttPahoClientFactory mqttClientFactory() {

		DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
		factory.setServerURIs(new String[] { url });
		factory.setUserName(username);
		factory.setPassword(password);
		return factory;
	}

	@Bean
	@ServiceActivator(inputChannel = "mqttOutboundChannel")
	public MessageHandler mqttOutbound() {
		MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(clientId + "-" + qualifier,
				mqttClientFactory());
		messageHandler.setAsync(true);
		messageHandler.setDefaultTopic(producerTopic);
		return messageHandler;
	}

	@Bean
	public MessageChannel mqttOutboundChannel() {
		return new FixedSubscriberChannel(mqttOutbound());
	}

	@MessagingGateway(defaultRequestChannel = "mqttOutboundChannel")
	public interface MqttProducer {

		void message(String data);

	}

}
