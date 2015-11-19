package com.covisint.platform.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.covisint.platform.device.client.attributetype.AttributeTypeSDK;
import com.covisint.platform.device.client.attributetype.AttributeTypeSDK.AttributeTypeClient;
import com.covisint.platform.device.client.commandtemplate.CommandTemplateSDK;
import com.covisint.platform.device.client.commandtemplate.CommandTemplateSDK.CommandTemplateClient;
import com.covisint.platform.device.client.devicetemplate.DeviceTemplateSDK;
import com.covisint.platform.device.client.devicetemplate.DeviceTemplateSDK.DeviceTemplateClient;
import com.covisint.platform.device.client.eventtemplate.EventTemplateSDK;
import com.covisint.platform.device.client.eventtemplate.EventTemplateSDK.EventTemplateClient;

@Component
public final class HttpClientFactory {

	@Value("${http.attribute_type_service_url}")
	private String attributeTypeServiceBaseUrl;

	@Value("${http.command_template_service_url}")
	private String commandTemplateServiceBaseUrl;

	@Value("${http.event_template_service_url}")
	private String eventTemplateServiceBaseUrl;

	@Value("${http.device_template_service_url}")
	private String deviceTemplateServiceBaseUrl;

	@Bean
	public AttributeTypeClient attributeTypeClient() {
		return new AttributeTypeSDK(attributeTypeServiceBaseUrl).newClient();
	}

	@Bean
	public CommandTemplateClient commandTemplateClient() {
		return new CommandTemplateSDK(commandTemplateServiceBaseUrl).newClient();
	}

	@Bean
	public EventTemplateClient eventTemplateClient() {
		return new EventTemplateSDK(eventTemplateServiceBaseUrl).newClient();
	}

	@Bean
	public DeviceTemplateClient deviceTemplateClient() {
		return new DeviceTemplateSDK(deviceTemplateServiceBaseUrl).newClient();
	}

}
