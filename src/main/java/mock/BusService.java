package mock;

import org.alljoyn.bus.BusException;

public class BusService implements PiBusInterface {

	@Override
	public void internalTempChanged(double temp) throws BusException {
		System.out.println("Internal temperature changed to " + temp);
	}

	@Override
	public void buzzerTurnedOn() throws BusException {
		System.out.println("Buzzer turned on");
	}

	@Override
	public void buzzerTurnedOff() throws BusException {
		System.out.println("Buzzer turned off");
	}

	@Override
	public String ledColorChanged() throws BusException {
		return "";
	}

	@Override
	public double getInternalTemp() throws BusException {
		System.out.println("Server: fetching internal temp...");
		return 0d;
	}

	@Override
	public int getBuzzerState() throws BusException {
		return 0;
	}

	@Override
	public String getLedColor() throws BusException {
		return "";
	}

	@Override
	public String ping(String text) throws BusException {
		System.out.println("PING! " + text);
		return text;
	}

	@Override
	public void setTargetTemp(double targetTemp) throws BusException {
		System.out.println("Setting target temp to " + targetTemp);
	}

	@Override
	public void turnOffBuzzer() throws BusException {
	}

	@Override
	public void turnOnBuzzer() throws BusException {
	}

}