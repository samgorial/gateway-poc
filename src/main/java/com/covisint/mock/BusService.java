package com.covisint.mock;

import org.alljoyn.bus.BusException;

public class BusService implements PiBusInterface {

	public void internalTempChanged(double temp) throws BusException {
		System.out.println("Internal temperature changed to " + temp);
	}

	public void buzzerTurnedOn() throws BusException {
		System.out.println("Buzzer turned on");
	}

	public void buzzerTurnedOff() throws BusException {
		System.out.println("Buzzer turned off");
	}

	public String ledColorChanged() throws BusException {
		System.out.println("Led color changed.");
		return "";
	}

	public double getInternalTemp() throws BusException {
		System.out.println("Server: fetching internal temp...");
		return 0d;
	}

	public int getBuzzerState() throws BusException {
		System.out.println("Getting buzzer state.");
		return 0;
	}

	public String getLedColor() throws BusException {
		System.out.println("Getting led color.");
		return "";
	}

	public void setTargetTemp(double targetTemp) throws BusException {
		System.out.println("Setting target temp to " + targetTemp);
	}

	public void turnOffBuzzer() throws BusException {
		System.out.println("Turning off buzzer.");
	}

	public void turnOnBuzzer() throws BusException {
		System.out.println("Turning on buzzer.");
	}

}