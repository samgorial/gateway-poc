package com.covisint.platform.gateway.proxygen;

import java.net.URL;

import org.alljoyn.bus.BusObject;

public interface ProxyClassGenerator {

	Class<? extends BusObject> generateAndExport(ProxySpec spec, URL output);

}
