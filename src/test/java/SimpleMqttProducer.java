import java.util.Random;
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

public class SimpleMqttProducer implements MqttCallback {

	MqttClient myClient;
	MqttConnectOptions connOpt;

	static final String BROKER_URL = "ssl://mqtt.stg.covapp.io:8883";
	static final String M2MIO_USERNAME = "e6191e5e-ba48-4ce7-bfe4-212ac3895e43";
	static final String M2MIO_PASSWORD_MD5 = "4458d6be-13ed-4c0a-afd4-682753a97007";
	static Random r = new Random(System.currentTimeMillis());
	
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
		SimpleMqttProducer smc = new SimpleMqttProducer();
		smc.runClient();
	}

	public void runClient() {
		// setup MQTT Client
		String clientID = "04A8bdE3BE7B4ACe964C" + "-producer";
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

		String myTopic = "1383cf27-f341-4fe7-a382-cd8f407f9a11";

		MqttTopic topic = myClient.getTopic(myTopic);


		try {

			for (;;) {

				JsonObjectBuilder payload = Json.createObjectBuilder();

				payload.add("messageId", UUID.randomUUID().toString());
				payload.add("deviceId", "301014c2-9385-49fe-a8f2-b93a240a7160");
//				payload.add("deviceId", "8566a97e-4861-4d4a-9928-69ab75d59f3b");
				payload.add("eventTemplateId", "332edae6-e0b9-4744-bba9-5e50f2090ab1");
//				payload.add("eventTemplateId", "cf239b04-0bc1-4c2e-8a4e-d3c1d2d0d4c0");

				JsonObjectBuilder args = Json.createObjectBuilder();
				args.add("newTemp", (r.nextFloat() * 100) / 10000);

				payload.add("message", Base64.encodeBase64String(args.build().toString().getBytes()));

				payload.add("encodingType", "base64");
				
				MqttMessage message = new MqttMessage(payload.build().toString().getBytes());
				message.setQos(1);
				message.setRetained(false);

				System.out.println(message);
				
				MqttDeliveryToken token = topic.publish(message);
				token.waitForCompletion();
				Thread.sleep(1000);

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}