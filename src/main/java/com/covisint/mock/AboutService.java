package com.covisint.mock;

import static com.covisint.platform.gateway.util.AllJoynSupport.getDefaultSessionOpts;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.alljoyn.bus.AboutObj;
import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.Status;

public class AboutService {
	static {
		System.loadLibrary("alljoyn_java");
	}

	static ExecutorService executor = Executors.newCachedThreadPool();

	private static final short CONTACT_PORT = 42;

	public static final Map<String, String> DEVICE_INTERFACE_MAP = new HashMap<>();

	static {
		DEVICE_INTERFACE_MAP.put("com.covisint.mock.PingInterface", "587f72be0167");
		DEVICE_INTERFACE_MAP.put("com.covisint.mock.SayHelloInterface", "8455ea8f488f");
		DEVICE_INTERFACE_MAP.put("com.covisint.mock.PiBusInterface", "a3f6e76cbfbc");
		DEVICE_INTERFACE_MAP.put("com.covisint.platform.device.demo.DemoInterface", "c963ba8bae3a");
	}

	public static void main(String[] args) {

		BusAttachment mBus = new BusAttachment("AppName", BusAttachment.RemoteMessage.Receive);

		final PiBusInterface service = new BusService();

		Status status = mBus.registerBusObject(service, "/example/path");

		if (status != Status.OK) {
			return;
		}

		status = mBus.registerBusObject(new MultiService(), "/multi");

		if (status != Status.OK) {
			return;
		}

		System.out.println("BusAttachment.registerBusObject successful");

		mBus.registerBusListener(new BusListener());

		status = mBus.connect();

		if (status != Status.OK) {
			return;
		}

		System.out.println("BusAttachment.connect successful on " + System.getProperty("org.alljoyn.bus.address"));

		Mutable.ShortValue contactPort = new Mutable.ShortValue(CONTACT_PORT);

		status = mBus.bindSessionPort(contactPort, getDefaultSessionOpts(), new SessionPortListener() {

			public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
				System.out.println("SessionPortListener.acceptSessionJoiner called");
				if (sessionPort == CONTACT_PORT) {
					return true;
				} else {
					return false;
				}
			}

			public void sessionJoined(short sessionPort, final int id, final String joiner) {
				System.out.println(
						String.format("SessionPortListener.sessionJoined(%d, %d, %s)", sessionPort, id, joiner));

				executor.submit(new Runnable() {

					public void run() {
						SignalEmitter emitter = new SignalEmitter(service, joiner, id,
								SignalEmitter.GlobalBroadcast.On);

						PiBusInterface myInterface = emitter.getInterface(PiBusInterface.class);

						try {
							while (true) {
								myInterface.buzzerTurnedOn();
								Thread.sleep(5000);
								myInterface.buzzerTurnedOff();
								Thread.sleep(5000);
							}
						} catch (BusException | InterruptedException e) {
							e.printStackTrace();
						}

					}
				});

			}

		});

		if (status != Status.OK) {
			return;
		}

		AboutObj aboutObj = new AboutObj(mBus);

		status = aboutObj.announce(contactPort.value, new MyAboutData());

		if (status != Status.OK) {
			System.out.println("Announce failed " + status.toString());
			return;
		}

		System.out.println("Announce called announcing SessionPort: " + contactPort.value);

		System.out.println("BusAttachment session established");

		while (true) {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				System.out.println("Thread Exception caught");
				e.printStackTrace();
			}
		}

	}
}
