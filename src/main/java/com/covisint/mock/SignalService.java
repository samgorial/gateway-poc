package com.covisint.mock;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.Status;

import com.covisint.platform.gateway.util.AllJoynSupport;

public class SignalService {

	private static final String ADVERTISED_NAME = "com.covisint.platform.devices.pi";

	static {
		System.loadLibrary("alljoyn_java");
	}

	private static final short CONTACT_PORT = 42;

	private static com.covisint.mock.PiBusInterface myInterface;

	static boolean mSessionEstablished = false;
	static int mSessionId;
	static String mJoinerName;

	private static class MyBusListener extends BusListener {
		public void nameOwnerChanged(String busName, String previousOwner, String newOwner) {
			if (ADVERTISED_NAME.equals(busName)) {
				System.out
						.println("BusAttachement.nameOwnerChanged(" + busName + ", " + previousOwner + ", " + newOwner);
			}
		}
	}

	public static void main(String[] args) {

		BusAttachment mBus = new BusAttachment("AppName", BusAttachment.RemoteMessage.Receive);

		Status status = mBus.registerBusObject(new BusService(), "/example/path");

		if (status != Status.OK) {
			return;
		}

		System.out.println("BusAttachment.registerBusObject successful");

		mBus.registerBusListener(new MyBusListener());

		status = mBus.connect();

		if (status != Status.OK) {
			return;
		}

		System.out.println("BusAttachment.connect successful on " + System.getProperty("org.alljoyn.bus.address"));

		Mutable.ShortValue contactPort = new Mutable.ShortValue(CONTACT_PORT);

		status = mBus.bindSessionPort(contactPort, AllJoynSupport.getDefaultSessionOpts(), new SessionPortListener() {

			public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
				System.out.println("SessionPortListener.acceptSessionJoiner called");
				if (sessionPort == CONTACT_PORT) {
					return true;
				} else {
					return false;
				}
			}

			public void sessionJoined(short sessionPort, int id, String joiner) {
				System.out.println(
						String.format("SessionPortListener.sessionJoined(%d, %d, %s)", sessionPort, id, joiner));
				mSessionId = id;
				mJoinerName = joiner;
				mSessionEstablished = true;
			}
		});

		if (status != Status.OK) {
			return;
		}

		System.out.println("BusAttachment.bindSessionPort successful");

		int flags = 0; // do not use any request name flags
		status = mBus.requestName(ADVERTISED_NAME, flags);
		
		if (status != Status.OK) {
			return;
		}
		
		System.out.println("BusAttachment.request '" + ADVERTISED_NAME + "' successful");

		status = mBus.advertiseName(ADVERTISED_NAME, SessionOpts.TRANSPORT_ANY);
		
		if (status != Status.OK) {
			System.out.println("Status = " + status);
			mBus.releaseName(ADVERTISED_NAME);
			return;
		}
		
		System.out.println("BusAttachment.advertiseName '" + ADVERTISED_NAME + "' successful");

		try {
			while (!mSessionEstablished) {
				Thread.sleep(10);
			}

			System.out.println(String.format("SignalEmitter sessionID = %d", mSessionId));

			SignalEmitter emitter = new SignalEmitter(new BusService(), mJoinerName, mSessionId,
					SignalEmitter.GlobalBroadcast.On);

			myInterface = emitter.getInterface(com.covisint.mock.PiBusInterface.class);

			while (true) {
				myInterface.buzzerTurnedOn();
				Thread.sleep(5000);
				myInterface.buzzerTurnedOff();
				Thread.sleep(5000);
			}
		} catch (InterruptedException ex) {
			System.out.println("Interrupted");
		} catch (BusException ex) {
			System.out.println("Bus Exception: " + ex.toString());
		}
	}
}
