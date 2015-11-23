import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class SimpleMqttConsumer implements MqttCallback {

	MqttClient myClient;
	MqttConnectOptions connOpt;

	static final String BROKER_URL = "ssl://mqtt.stg.covapp.io:8883";
	static final String M2MIO_USERNAME = "b80b3e8f-2e08-4710-a63f-5a74cd324f37";
	static final String M2MIO_PASSWORD_MD5 = "65e8e8c3-2b2a-4f43-8f75-3d5bcbb1deb4";

	public void connectionLost(Throwable t) {
		System.out.println("Connection lost!");
	}

	public void deliveryComplete(IMqttDeliveryToken token) {
	}

	public void messageArrived(String topicName, MqttMessage message) throws Exception {
		System.out.println("-------------------------------------------------");
		System.out.println("| Topic:" + topicName);
		System.out.println("| Message: " + new String(message.getPayload()));
		System.out.println("-------------------------------------------------");
	}

	public static void main(String[] args) {
		SimpleMqttConsumer smc = new SimpleMqttConsumer();
		smc.runClient();
	}

	public void runClient() {
		// setup MQTT Client
		String clientID = "Ad7b8e20285D4D2097a3" + "-consumer";
		connOpt = new MqttConnectOptions();

		connOpt.setCleanSession(true);
		connOpt.setKeepAliveInterval(30);
		connOpt.setUserName(M2MIO_USERNAME);
		connOpt.setPassword(M2MIO_PASSWORD_MD5.toCharArray());

		// Connect to Broker
		try {
			myClient = new MqttClient(BROKER_URL, clientID);
			myClient.setCallback(this);
			myClient.connect(connOpt);
		} catch (MqttException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		System.out.println("Connected to " + BROKER_URL);

		String myTopic = "e1baeafb-1cc8-4b1e-9297-de53096e20ea";

		try {
			int subQoS = 0;
			myClient.subscribe(myTopic, subQoS);
		} catch (Exception e) {
			e.printStackTrace();
		}

		for (;;) {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}
}