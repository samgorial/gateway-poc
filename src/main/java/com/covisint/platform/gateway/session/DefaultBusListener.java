package com.covisint.platform.gateway.session;

import static com.covisint.platform.gateway.util.AllJoynSupport.getDefaultSessionOpts;

import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.covisint.platform.gateway.GatewayBus;

import mock.Globals;
import mock.Globals.SessionInfo;

@Component
public class DefaultBusListener extends BusListener {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultBusListener.class);

	@Value("${advertised.name.pfx}")
	private String advertisedNamePrefix;

	private final short port = 42; // TODO fix this

	@Autowired
	private GatewayBus bus;

	public void lostAdvertisedName(String name, short transport, String namePrefix) {
		super.lostAdvertisedName(name, transport, namePrefix);
		
		System.err.println("Lost advertised name " + name + ":" + transport + ":" + namePrefix);
	}

	public void foundAdvertisedName(String name, short transport, String namePrefix) {

		LOG.debug("Found advertised name.  name: {}, transport: {}, name prefix: {}", name, transport, namePrefix);

		Mutable.IntegerValue sessionId = new Mutable.IntegerValue();

		bus.getBusAttachment().enableConcurrentCallbacks();

		Status status = bus.getBusAttachment().joinSession(name, port, sessionId, getDefaultSessionOpts(),
				new DefaultSessionListener());

		if (status != Status.OK) {
			LOG.error("Could not join bus session; reason: {}", status);
			return;
		}

		LOG.debug("Joined bus session with session id {}", sessionId.value);

		SessionInfo sessionInfo = new SessionInfo();
		sessionInfo.sessionId = sessionId.value;
		sessionInfo.busName = name;
		sessionInfo.namePrefix = namePrefix;
		sessionInfo.port = port;
		sessionInfo.transport = transport;
		sessionInfo.path = "???";
		sessionInfo.sessionOpts = getDefaultSessionOpts();

		Globals.addSession(sessionInfo);
	}

	public void nameOwnerChanged(String busName, String previousOwner, String newOwner) {
		LOG.debug("Owner changed; bus name: {}, previous owner: {}, new owner: {}", busName, previousOwner, newOwner);
	}

}