package com.covisint.mock;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.annotation.BusAnnotation;
import org.alljoyn.bus.annotation.BusAnnotations;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusMethod;
import org.alljoyn.bus.annotation.BusProperty;
import org.alljoyn.bus.annotation.BusSignal;

@BusInterface(announced = "true", description = " An way of saying hello!")
public interface SayHelloInterface extends BusObject {

	@BusProperty
	String getId() throws BusException;

	@BusMethod
	@BusAnnotations({ @BusAnnotation(name = "arg0", value = "name") })
	void hello(String name) throws BusException;

	@BusSignal
	@BusAnnotations({ @BusAnnotation(name = "arg0", value = "newStatus") })
	String statusChanged() throws BusException;

}