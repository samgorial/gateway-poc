package com.covisint.platform.gateway;

import static com.covisint.platform.gateway.util.AllJoynSupport.validateCommand;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonStructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

import com.covisint.platform.gateway.bind.CommandDelegate;

@Component
public class GatewayMqttService {

	private static final Logger LOG = LoggerFactory.getLogger(GatewayMqttService.class);

	@Value("${mqtt.url}")
	private String url;

	@Value("${mqtt.username}")
	private String username;

	@Value("${mqtt.password}")
	private String password;

	@Value("${mqtt.clientid}")
	private String clientId;

	@Value("${mqtt.topics}")
	private String[] topics;

	@Value("${mqtt.completion_timeout}")
	private int completionTimeout;

	@Value("${mqtt.default_qos}")
	private int defaultQos;

	@Autowired
	private CommandDelegate delegate;

	@Bean
	public MessageChannel mqttInputChannel() {
		return new FixedSubscriberChannel(handler());
	}

	@Bean
	public MessageProducer inbound() {
		DefaultMqttPahoClientFactory clientFactory = new DefaultMqttPahoClientFactory();
		clientFactory.setUserName(username);
		clientFactory.setPassword(password);

		MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(url, clientId,
				clientFactory, topics);
		adapter.setCompletionTimeout(completionTimeout);
		adapter.setConverter(new DefaultPahoMessageConverter());
		adapter.setQos(defaultQos);
		adapter.setOutputChannel(mqttInputChannel());
		return adapter;
	}

	@Bean
	@ServiceActivator(inputChannel = "mqttInputChannel")
	public MessageHandler handler() {
		return new MessageHandler() {

			public void handleMessage(Message<?> message) throws MessagingException {

				String payload = (String) message.getPayload();

				LOG.debug("Processing command: \n{}", payload);

				// FIXME accept JSON directly instead of parsing strings.
				JsonReader reader = Json.createReader(new StringReader(payload));

				JsonStructure json = reader.read();

				JsonObject command = validateCommand(json);

				delegate.process(command);

				LOG.debug("Successfully processed command.");
			}

		};
	}

}
