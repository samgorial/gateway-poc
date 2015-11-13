package about;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusMethod;
import org.alljoyn.bus.annotation.BusProperty;
import org.alljoyn.bus.annotation.BusSignal;

@BusInterface(name = "com.covisint.platform.devices.nest", announced = "true")
public interface PiBusInterface extends BusObject {

	@BusProperty
	public double getInternalTemp() throws BusException;

	@BusProperty
	public int getBuzzerState() throws BusException;

	@BusProperty
	public String getLedColor() throws BusException;

	@BusMethod
	public String ping(String text) throws BusException;

	@BusMethod
	public void setTargetTemp(double targetTemp) throws BusException;

	@BusMethod
	public void turnOnBuzzer() throws BusException;

	@BusMethod
	public void turnOffBuzzer() throws BusException;

	@BusSignal
	public void internalTempChanged(double temp) throws BusException;

	@BusSignal
	public String ledColorChanged() throws BusException;

	@BusSignal
	public void buzzerTurnedOn() throws BusException;

	@BusSignal
	public void buzzerTurnedOff() throws BusException;

}