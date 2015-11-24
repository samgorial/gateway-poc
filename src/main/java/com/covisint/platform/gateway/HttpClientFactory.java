package com.covisint.platform.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.covisint.platform.device.client.attributetype.AttributeTypeSDK;
import com.covisint.platform.device.client.attributetype.AttributeTypeSDK.AttributeTypeClient;
import com.covisint.platform.device.client.commandtemplate.CommandTemplateSDK;
import com.covisint.platform.device.client.commandtemplate.CommandTemplateSDK.CommandTemplateClient;
import com.covisint.platform.device.client.device.DeviceSDK;
import com.covisint.platform.device.client.device.DeviceSDK.DeviceClient;
import com.covisint.platform.device.client.devicetemplate.DeviceTemplateSDK;
import com.covisint.platform.device.client.devicetemplate.DeviceTemplateSDK.DeviceTemplateClient;
import com.covisint.platform.device.client.eventtemplate.EventTemplateSDK;
import com.covisint.platform.device.client.eventtemplate.EventTemplateSDK.EventTemplateClient;
import com.covisint.platform.eventSource.client.eventSource.EventSourceSDK;
import com.covisint.platform.eventSource.client.eventSource.EventSourceSDK.EventSourceClient;
import com.covisint.platform.messaging.stream.client.sdk.StreamDeviceSDK;
import com.covisint.platform.messaging.stream.client.sdk.StreamDeviceSDK.StreamDeviceClient;

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

	@Value("${http.device_service_url}")
	private String deviceServiceBaseUrl;

	@Value("${http.stream_device_service_url}")
	private String streamDeviceServiceBaseUrl;

	@Value("${http.event_source_service_url}")
	private String eventSourceServiceBaseUrl;

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

	@Bean
	public DeviceClient deviceClient() {
		return new DeviceSDK(deviceServiceBaseUrl).newClient();
	}

	@Bean
	public StreamDeviceClient streamDeviceClient() {
		return new StreamDeviceSDK(streamDeviceServiceBaseUrl).newClient();
	}

	@Bean
	public EventSourceClient eventSourceClient() {
		return new EventSourceSDK(eventSourceServiceBaseUrl).newClient();
	}

}
