package com.covisint.platform.gateway.signal;

import static com.covisint.platform.gateway.util.AllJoynSupport.getDefaultSessionOpts;

import javax.annotation.PostConstruct;

import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.covisint.platform.gateway.GatewayBus;

@Component
public class DefaultSignalListener extends BusListener {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultSignalListener.class);

	private final short port = 42; // TODO fix this

	@Autowired
	private GatewayBus bus;

	@PostConstruct
	public void init() {
		bus.getCentralBusAttachment().registerBusListener(this);
	}

	public void foundAdvertisedName(String name, short transport, String namePrefix) {

		LOG.debug("Found advertised name.  name: {}, transport: {}, name prefix: {}", name, transport, namePrefix);

		Mutable.IntegerValue sessionId = new Mutable.IntegerValue();

		bus.getCentralBusAttachment().enableConcurrentCallbacks();

		Status status = bus.getCentralBusAttachment().joinSession(name, port, sessionId, getDefaultSessionOpts(),
				new SessionListener());

		if (status != Status.OK) {
			LOG.error("Could not join bus session; reason: {}", status);
			return;
		}

		LOG.debug("Joined bus session with session id {}", sessionId.value);
	}

	public void nameOwnerChanged(String busName, String previousOwner, String newOwner) {
		LOG.debug("Owner changed; bus name: {}, previous owner: {}, new owner: {}", busName, previousOwner, newOwner);
	}

}