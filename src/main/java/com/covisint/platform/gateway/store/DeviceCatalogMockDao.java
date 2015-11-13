package com.covisint.platform.gateway.store;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.covisint.platform.device.core.device.Device;
import com.covisint.platform.gateway.domain.alljoyn.AJInterface;

@Component
public class DeviceCatalogMockDao implements DeviceCatalogDao {

	public final Map<AJInterface, Device> deviceMap = new HashMap<>();

	public Device searchByInterface(AJInterface intf) {
		return deviceMap.get(intf);
	}

}
