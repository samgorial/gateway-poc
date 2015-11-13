package com.covisint.platform.gateway.store;

import org.springframework.stereotype.Component;

import com.covisint.platform.device.core.device.Device;
import com.covisint.platform.gateway.domain.alljoyn.AJInterface;

@Component
public class DeviceProvisionerMockDao implements DeviceProvisionerDao {

	public Device provisionNewDevice(AJInterface intf) {
		throw new UnsupportedOperationException();
	}

}
