package com.covisint.platform.gateway.bind;

import javax.json.JsonObject;
import javax.json.JsonString;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.ProxyBusObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.covisint.platform.gateway.GatewayBus;

import mock.PiBusInterface;

@Component
public class HardcodedCommandDelegate implements CommandDelegate {

	@Autowired
	private GatewayBus bus;

	public void process(JsonObject command) {

		JsonString deviceId = command.getJsonString("deviceId");

		JsonString intf = command.getJsonString("interface");

		JsonString commandName = command.getJsonString("name");

		// JsonString message = command.getJsonString("message");
		// JsonReader reader = Json.createReader(new
		// ByteArrayInputStream(Base64.decodeBase64(message.getString())));
		// JsonObject payload = reader.readObject();

		ProxyBusObject proxy = bus.getBusAttachment().getProxyBusObject("", "", 1,
				new Class<?>[] { PiBusInterface.class });

		PiBusInterface service = proxy.getInterface(PiBusInterface.class);

		try {

			switch (commandName.getString()) {
			case "ping":
				service.ping("" + System.currentTimeMillis());
				break;
			case "turn_on_buzzer":
				service.turnOnBuzzer();
				break;
			case "turn_off_buzzer":
				service.turnOffBuzzer();
				break;
			default:
				throw new UnsupportedOperationException(commandName.getString());
			}

		} catch (BusException e) {
			throw new RuntimeException(e);
		}

	}

}
