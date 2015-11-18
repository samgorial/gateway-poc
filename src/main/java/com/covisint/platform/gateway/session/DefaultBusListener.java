package com.covisint.platform.gateway.session;

import static com.covisint.platform.gateway.util.AllJoynSupport.getDefaultSessionOpts;

import java.util.List;

import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.covisint.platform.gateway.GatewayBus;
import com.covisint.platform.gateway.repository.SessionEndpoint;
import com.covisint.platform.gateway.repository.SessionInfo;
import com.covisint.platform.gateway.repository.SessionInfo.SessionType;
import com.covisint.platform.gateway.repository.SessionRepository;

@Component
public class DefaultBusListener extends BusListener {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultBusListener.class);

	@Value("${alljoyn.advertised_name_pfx}")
	private String advertisedNamePrefix;

	@Value("${mock.device.id}")
	private String mockDeviceId;

	@Autowired
	private SessionListener sessionListener;

	@Autowired
	private GatewayBus bus;

	@Autowired
	private SessionRepository sessionRepository;

	public void lostAdvertisedName(String name, short transport, String namePrefix) {
		super.lostAdvertisedName(name, transport, namePrefix);

		List<SessionEndpoint> endpoints = sessionRepository.getEndpointsByInterface(name, SessionType.STANDARD);

		if (endpoints == null || endpoints.isEmpty()) {
			throw new IllegalStateException("No session endpoints found for interface " + name);
		}

		if (endpoints.size() > 1) {
			LOG.warn("Found multiple endpoints for interface {}", name);
		}

		SessionEndpoint endpoint = endpoints.get(0);

		endpoint.setAssociatedDeviceId(null);

		LOG.debug("Lost advertised name: name[{}], transport[{}], prefix[{}]", name, transport, namePrefix);
	}

	public void foundAdvertisedName(String name, short transport, String namePrefix) {

		LOG.debug("Found advertised name.  name: {}, transport: {}, name prefix: {}", name, transport, namePrefix);

		Mutable.IntegerValue sessionIdWrapper = new Mutable.IntegerValue();

		bus.getBusAttachment().enableConcurrentCallbacks();

		List<SessionEndpoint> endpoints = sessionRepository.getEndpointsByInterface(name, SessionType.ABOUT);

		if (endpoints.isEmpty()) {
			throw new IllegalStateException("No session endpoints found for interface " + name);
		}

		if (endpoints.size() > 1) {
			LOG.warn("Found multiple endpoints for interface {}", name);
		}

		List<SessionEndpoint> standardEndpoints = sessionRepository.getEndpointsByInterface(name, SessionType.STANDARD);

		if (!standardEndpoints.isEmpty()) {
			LOG.info("Already joined a session for name {}, not joining again.: \n{}", name,
					standardEndpoints.get(0).getParentSession());
			return;
		}

		// FIXME handle multiple endpoint matches
		short port = endpoints.get(0).getParentSession().getPort();
		String path = endpoints.get(0).getPath();

		Status status = bus.getBusAttachment().joinSession(name, port, sessionIdWrapper, getDefaultSessionOpts(),
				sessionListener);

		if (status != Status.OK) {
			LOG.error("Could not join bus session; reason: {}", status);
			return;
		}

		int sessionId = sessionIdWrapper.value;

		LOG.debug("Joined bus session with session id {}", sessionId);

		SessionInfo sessionInfo = new SessionInfo();
		sessionInfo.setSessionId(sessionId);
		sessionInfo.setBusName(name);
		sessionInfo.setPort(port);
		sessionInfo.setSessionType(SessionType.STANDARD);
		sessionInfo.setSessionOpts(getDefaultSessionOpts());

		SessionEndpoint endpoint = new SessionEndpoint();
		endpoint.setParentSession(sessionInfo);
		endpoint.setIntf(name);
		endpoint.setPath(path);
		sessionInfo.getEndpoints().add(endpoint);

		sessionRepository.createSession(sessionInfo);

		sessionRepository.setDeviceSession(mockDeviceId, sessionId, name);
	}

	public void nameOwnerChanged(String busName, String previousOwner, String newOwner) {
		LOG.debug("Owner changed; bus name[{}], previous owner[{}], new owner[{}]", busName, previousOwner, newOwner);
	}

}