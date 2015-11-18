package mock;

import org.alljoyn.bus.AboutObj;
import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.Status;
import static com.covisint.platform.gateway.util.AllJoynSupport.getDefaultSessionOpts;

public class AboutService {
	static {
		System.loadLibrary("alljoyn_java");
	}

	private static final short CONTACT_PORT = 42;

	static boolean sessionEstablished = false;
	static int sessionId;

	public static void main(String[] args) {

		BusAttachment mBus = new BusAttachment("AppName", BusAttachment.RemoteMessage.Receive);

		Status status = mBus.registerBusObject(new BusService(), "/example/path");
		
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

			public void sessionJoined(short sessionPort, int id, String joiner) {
				System.out.println(
						String.format("SessionPortListener.sessionJoined(%d, %d, %s)", sessionPort, id, joiner));
				sessionId = id;
				sessionEstablished = true;
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

		while (!sessionEstablished) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				System.out.println("Thread Exception caught");
				e.printStackTrace();
			}
		}
		
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
