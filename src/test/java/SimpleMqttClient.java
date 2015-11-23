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

public class SimpleMqttClient implements MqttCallback {

	MqttClient myClient;
	MqttConnectOptions connOpt;

	static final String BROKER_URL = "ssl://mqtt.stg.covapp.io:8883";
	static final String M2MIO_USERNAME = "e6191e5e-ba48-4ce7-bfe4-212ac3895e43";
	static final String M2MIO_PASSWORD_MD5 = "4458d6be-13ed-4c0a-afd4-682753a97007";

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
		SimpleMqttClient smc = new SimpleMqttClient();
		smc.runClient();
	}

	public void runClient() {
		// setup MQTT Client
		String clientID = "04A8bdE3BE7B4ACe964C" + "-publisher";
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

		String myTopic = "e786d291-e8f0-4e3c-93d5-a261d55c91bd";
		MqttTopic topic = myClient.getTopic(myTopic);

		JsonObjectBuilder payload = Json.createObjectBuilder();

		payload.add("messageId", UUID.randomUUID().toString());
		payload.add("deviceId", "e9992d80-10a5-4c6c-8bf0-4ee0b442afa6");
		payload.add("commandTemplateId", "be92536c-4078-42f6-a28a-ce9b5575f829");

		JsonObjectBuilder args = Json.createObjectBuilder();
		args.add("targetTemp", 85.1);

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