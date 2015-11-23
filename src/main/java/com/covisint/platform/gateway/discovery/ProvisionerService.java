package com.covisint.platform.gateway.discovery;

import java.util.Map;

import org.alljoyn.bus.Variant;

import com.covisint.platform.device.core.device.Device;
import com.covisint.platform.device.core.devicetemplate.DeviceTemplate;
import com.covisint.platform.gateway.domain.AJInterface;

public interface ProvisionerService {

	DeviceTemplate searchDeviceTemplates(AJInterface intf);

	DeviceTemplate createDeviceTemplate(AJInterface intf);

	Device searchDevices(String deviceTemplateId, Map<String, Variant> aboutData);

	Device createDevice(String deviceTemplateId, Map<String, Variant> aboutData);

	void deactivateProvisionedComponents();
	
}
