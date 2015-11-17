package com.covisint.platform.gateway.repository;

import com.covisint.platform.device.core.device.Device;
import com.covisint.platform.gateway.domain.alljoyn.AJInterface;

public interface DeviceCatalogDao {

	Device searchByInterface(AJInterface intf);
	
}
