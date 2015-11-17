package com.covisint.platform.gateway.session;

import static com.covisint.platform.gateway.util.AllJoynSupport.getDefaultSessionOpts;

import java.util.Map;

import org.alljoyn.bus.AboutListener;
import org.alljoyn.bus.AboutObjectDescription;
import org.alljoyn.bus.AboutProxy;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.Variant;
import org.alljoyn.bus.ifaces.AllSeenIntrospectable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.covisint.platform.gateway.GatewayBus;
import com.covisint.platform.gateway.discovery.DiscoveryService;
import com.covisint.platform.gateway.discovery.IntrospectResult;
import com.covisint.platform.gateway.discovery.Introspector;
import com.covisint.platform.gateway.repository.SessionEndpoint;
import com.covisint.platform.gateway.repository.SessionInfo;
import com.covisint.platform.gateway.repository.SessionInfo.SessionType;
import com.covisint.platform.gateway.repository.SessionRepository;

@Component
public class DefaultAboutListener implements AboutListener {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultAboutListener.class);

	@Value("${mock.device.id}")
	private String mockDeviceId;

	@Autowired
	private SessionListener sessionListener;

	@Autowired
	private GatewayBus bus;

	@Autowired
	private Introspector introspector;

	@Autowired
	private DiscoveryService discoveryService;

	@Autowired
	private SessionRepository sessionRepository;

	public void announced(String busName, int version, short port, AboutObjectDescription[] aods,
			Map<String, Variant> aboutData) {

		LOG.info("Announcement received: BusName[{}], Version[{}], SessionPort[{}]", busName, version, port);

		bus.getBusAttachment().enableConcurrentCallbacks();

		Mutable.IntegerValue sessionIdWrapper = new Mutable.IntegerValue();

		Status status = bus.getBusAttachment().joinSession(busName, port, sessionIdWrapper, getDefaultSessionOpts(),
				sessionListener);

		if (status != Status.OK) {
			return;
		}

		int sessionId = sessionIdWrapper.value;

		LOG.debug("Successfully joined About bus session with sessionId {}", sessionId);

		AboutProxy aboutProxy = new AboutProxy(bus.getBusAttachment(), busName, sessionId);

		SessionInfo sessionInfo = new SessionInfo();
		sessionInfo.setSessionId(sessionId);
		sessionInfo.setBusName(busName);
		sessionInfo.setPort(port);
		sessionInfo.setSessionType(SessionType.ABOUT);
		sessionInfo.setSessionOpts(getDefaultSessionOpts());

		try {

			AboutObjectDescription aod[] = aboutProxy.getObjectDescription();

			if (aod != null) {
				for (AboutObjectDescription o : aod) {

					SessionEndpoint endpoint = new SessionEndpoint();
					endpoint.setParentSession(sessionInfo);
					endpoint.setIntf(o.interfaces[0]); // FIXME persist all
					endpoint.setPath(o.path);
					sessionInfo.getEndpoints().add(endpoint);

					ProxyBusObject proxy = bus.getBusAttachment().getProxyBusObject(busName, o.path, sessionId,
							new Class<?>[] { AllSeenIntrospectable.class });

					AllSeenIntrospectable introspectable = proxy.getInterface(AllSeenIntrospectable.class);

					IntrospectResult introspectResult = introspector.doIntrospection(introspectable);

					discoveryService.handleAsync(introspectResult);

				}
			}

			sessionRepository.createSession(sessionInfo);

//			 FIXME temporary
//			setMockDeviceAssociations(sessionInfo);

			logSummary(busName, aboutProxy.getVersion(), port, aod, aboutProxy.getAboutData("en"));

		} catch (BusException e) {
			throw new RuntimeException("Error occurred while communicating with remote About object.", e);
		}

	}

//	private void setMockDeviceAssociations(SessionInfo sessionInfo) {
//		for (SessionEndpoint endpoint : sessionInfo.getEndpoints()) {
//			sessionRepository.setDeviceSession(mockDeviceId, sessionInfo.getSessionId(), endpoint.getIntf());
//		}
//	}

	private void logSummary(String busName, int version, short port, AboutObjectDescription[] aods,
			Map<String, Variant> aboutData) throws BusException {

		LOG.info("About announcement summary >>>");

		StringBuilder sb = new StringBuilder("\nAboutObjectDescription: {\n");

		if (aods != null) {
			for (AboutObjectDescription aod : aods) {
				sb.append("\t").append(aod.path).append("\n");
				for (String iface : aod.interfaces) {
					sb.append("\t\t").append(iface).append("\n");
				}
			}
			sb.append("\n}");
		}

		LOG.info(sb.toString());

		sb = new StringBuilder("\n");

		for (Map.Entry<String, Variant> entry : aboutData.entrySet()) {
			sb.append("\t").append(entry.getKey()).append(": ");

			if (entry.getKey().equals("AppId")) {
				byte[] appId = entry.getValue().getObject(byte[].class);
				for (byte b : appId) {
					sb.append(String.format("%02X", b));
				}
			} else if (entry.getKey().equals("SupportedLanguages")) {
				String[] supportedLanguages = entry.getValue().getObject(String[].class);
				for (String s : supportedLanguages) {
					sb.append(s).append(" ");
				}
			} else {
				sb.append(entry.getValue().getObject(String.class));
			}
			sb.append("\n");
		}

		LOG.info("AboutData variants for {} \n{}", busName, sb);
	}

}
