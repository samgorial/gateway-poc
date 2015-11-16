package mock;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusMethod;
import org.alljoyn.bus.annotation.BusProperty;
import org.alljoyn.bus.annotation.BusSignal;

@BusInterface(name = "com.covisint.platform.devices.nest", announced = "true")
public interface PiBusInterface extends BusObject {

	@BusProperty
	double getInternalTemp() throws BusException;

	@BusProperty
	int getBuzzerState() throws BusException;

	@BusProperty
	String getLedColor() throws BusException;

	@BusMethod
	String ping(String text) throws BusException;

	@BusMethod
	void setTargetTemp(double targetTemp) throws BusException;

	@BusMethod
	void turnOnBuzzer() throws BusException;

	@BusMethod
	void turnOffBuzzer() throws BusException;

	@BusSignal
	void internalTempChanged(double temp) throws BusException;

	@BusSignal
	String ledColorChanged() throws BusException;

	@BusSignal
	void buzzerTurnedOn() throws BusException;

	@BusSignal
	void buzzerTurnedOff() throws BusException;

}