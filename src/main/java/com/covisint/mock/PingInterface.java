package com.covisint.mock;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusMethod;

@BusInterface(announced = "true")
public interface PingInterface extends BusObject {

	@BusMethod
	void ping(String message) throws BusException;

}