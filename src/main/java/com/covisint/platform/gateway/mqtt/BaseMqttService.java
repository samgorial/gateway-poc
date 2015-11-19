package com.covisint.platform.gateway.mqtt;

import org.springframework.beans.factory.annotation.Value;

abstract class BaseMqttService {

	@Value("${mqtt.url}")
	protected String url;

	@Value("${mqtt.username}")
	protected String username;

	@Value("${mqtt.password}")
	protected String password;

	@Value("${mqtt.clientid}")
	protected String clientId;

	@Value("${mqtt.topics.consumer}")
	protected String consumerTopic;

	@Value("${mqtt.topics.producer}")
	protected String producerTopic;

	@Value("${mqtt.completion_timeout}")
	protected int completionTimeout;

	@Value("${mqtt.default_qos}")
	protected int defaultQos;

}
