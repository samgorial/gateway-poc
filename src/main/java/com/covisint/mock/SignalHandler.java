package com.covisint.mock;

import org.alljoyn.bus.annotation.BusSignalHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SignalHandler {

	private static final Logger LOG = LoggerFactory.getLogger(SignalHandler.class);

	@BusSignalHandler(iface = "com.covisint.mock.PiBusInterface", signal = "internalTempChanged")
	public void internalTempChanged(double temp) {
		LOG.info("Internal temperature changed to {}", temp);
	}

	@BusSignalHandler(iface = "com.covisint.mock.PiBusInterface", signal = "buzzerTurnedOn")
	public void buzzerTurnedOn() {
		LOG.info("Buzzer turned on");
	}

	@BusSignalHandler(iface = "com.covisint.mock.PiBusInterface", signal = "buzzerTurnedOff")
	public void buzzerTurnedOff() {
		LOG.info("Buzzer turned off");
	}

	@BusSignalHandler(iface = "com.covisint.mock.PiBusInterface", signal = "ledColorChanged")
	public String ledColorChanged(String newColor) {
		LOG.info("LED color changed to {}", newColor);
		return newColor;
	}

}
