package com.covisint.platform.gateway.discovery;

import com.covisint.platform.device.core.devicetemplate.DeviceTemplate;
import com.covisint.platform.gateway.domain.alljoyn.AJInterface;

public interface ProvisionerService {

	DeviceTemplate searchDeviceTemplates(AJInterface intf);

	DeviceTemplate createDeviceTemplate(AJInterface intf);

}
