import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

public class GatewayCommander implements MqttCallback {

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
		GatewayCommander smc = new GatewayCommander();
		smc.runClient();
	}

	public void runClient() {
		// setup MQTT Client
		String clientID = "Ad7b8e20285D4D2097a3" + "-producer";
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

		MqttTopic topic = myClient.getTopic(myTopic);

		JsonObjectBuilder payload = Json.createObjectBuilder();

		payload.add("messageId", UUID.randomUUID().toString());
		payload.add("deviceId", "c5dd8c99-4223-46b0-80d2-9b07b6776ece");
		payload.add("commandTemplateId", "55f93bf5-b13b-4794-90ad-4efb56d8663a");

		JsonObjectBuilder args = Json.createObjectBuilder();
		args.add("targetTemp", 8.1);

		payload.add("message", Base64.encodeBase64String(args.build().toString().getBytes()));

		MqttMessage message = new MqttMessage(payload.build().toString().getBytes());
		message.setQos(1);
		message.setRetained(false);

		try {
			MqttDeliveryToken token = topic.publish(message);
			token.waitForCompletion();
			Thread.sleep(100);

			myClient.disconnect();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}