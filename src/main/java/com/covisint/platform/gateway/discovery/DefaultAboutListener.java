package com.covisint.platform.gateway.discovery;

import static com.covisint.platform.gateway.util.AllJoynSupport.getDefaultSessionOpts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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
import org.springframework.stereotype.Component;

import com.covisint.platform.device.core.device.Device;
import com.covisint.platform.gateway.GatewayBus;
import com.covisint.platform.gateway.domain.AJInterface;
import com.covisint.platform.gateway.repository.session.AboutSession;
import com.covisint.platform.gateway.repository.session.SessionRepository;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

@Component
public class DefaultAboutListener implements AboutListener {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultAboutListener.class);

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

		LOG.debug("Successfully joined about bus session with sessionId {}", sessionId);

		AboutProxy aboutProxy = new AboutProxy(bus.getBusAttachment(), busName, sessionId);

		LOG.debug("Creating new AboutSession entry.");

		AboutSession sessionInfo = new AboutSession();
		sessionInfo.setSessionId(sessionId);
		sessionInfo.setBusName(busName);
		sessionInfo.setPort(port);
		sessionInfo.setSessionOpts(getDefaultSessionOpts());

		sessionRepository.createAboutSession(sessionInfo);

		try {

			AboutObjectDescription aod[] = aboutProxy.getObjectDescription();

			if (aod != null) {
				for (AboutObjectDescription o : aod) {

					String objectPath = o.path;

					ProxyBusObject proxy = bus.getBusAttachment().getProxyBusObject(busName, objectPath, sessionId,
							new Class<?>[] { AllSeenIntrospectable.class });

					AllSeenIntrospectable introspectable = proxy.getInterface(AllSeenIntrospectable.class);

					IntrospectResult introspectResult = introspector.doIntrospection(introspectable);

					List<AJInterface> interfaces = introspectResult.getInterfaces();

					LOG.debug("Asynchronously processing AJ metadata containing {} interfaces.", interfaces.size());

					List<ListenableFuture<Optional<Device>>> futures = new ArrayList<>();

					for (final AJInterface intf : interfaces) {
						// Set about-data map into the interface for downstream
						// processing.
						intf.setAboutData(aboutData);
						// Process each interface async.
						futures.add(discoveryService.processInterface(intf, sessionInfo, objectPath));
					}

					try {
						// Wait on all async tasks to finish.
						Futures.allAsList(futures).get();
					} catch (InterruptedException | ExecutionException e) {
						throw new RuntimeException("Error encountered while waiting for tasks to complete.", e);
					}

				}
			}

			// Done. Log summary.
			logSummary(busName, aboutProxy.getVersion(), port, aod, aboutProxy.getAboutData("en"));

		} catch (BusException e) {
			throw new RuntimeException("Error occurred while communicating with remote About object.", e);
		}

	}

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

	@Component
	private static class DefaultSessionListener extends SessionListener {

		private static final Logger LOG = LoggerFactory.getLogger(DefaultSessionListener.class);

		@Autowired
		private SessionRepository sessionRepository;

		public void sessionLost(int sessionId, int reason) {
			super.sessionLost(sessionId, reason);

			LOG.debug("Lost session {} due to {}", sessionId, reason);

			sessionRepository.deleteAboutSession(sessionId);
		}

		public void sessionMemberAdded(int sessionId, String uniqueName) {

			LOG.debug("Session member {} added to session {}", uniqueName, sessionId);

			super.sessionMemberAdded(sessionId, uniqueName);
		}

		public void sessionMemberRemoved(int sessionId, String uniqueName) {
			LOG.debug("Session member {} removed from session {}", uniqueName, sessionId);

			super.sessionMemberRemoved(sessionId, uniqueName);
		}

	}

}
