package com.covisint.mock;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusMethod;

@BusInterface(announced = "true")
public interface SayHelloInterface extends BusObject {

	@BusMethod
	void hello(String name) throws BusException;

}