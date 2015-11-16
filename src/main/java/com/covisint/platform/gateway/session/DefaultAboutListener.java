package com.covisint.platform.gateway.session;

import static com.covisint.platform.gateway.util.AllJoynSupport.getDefaultSessionOpts;

import java.util.Map;

import org.alljoyn.bus.AboutListener;
import org.alljoyn.bus.AboutObjectDescription;
import org.alljoyn.bus.AboutProxy;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.Variant;
import org.alljoyn.bus.ifaces.AllSeenIntrospectable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.covisint.platform.gateway.GatewayBus;
import com.covisint.platform.gateway.discovery.DiscoveryService;
import com.covisint.platform.gateway.discovery.IntrospectResult;
import com.covisint.platform.gateway.discovery.Introspector;

import mock.Globals;
import mock.Globals.SessionInfo;

@Component
public class DefaultAboutListener implements AboutListener {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultAboutListener.class);

	@Autowired
	private GatewayBus bus;

	@Autowired
	private Introspector introspector;

	@Autowired
	private DiscoveryService discoveryService;

	public void announced(String busName, int version, short port, AboutObjectDescription[] aods,
			Map<String, Variant> aboutData) {

		LOG.info("Announcement received: BusName[{}], Version[{}], SessionPort[{}]", busName, version, port);

		bus.getBusAttachment().enableConcurrentCallbacks();

		Mutable.IntegerValue sessionId = new Mutable.IntegerValue();

		Status status = bus.getBusAttachment().joinSession(busName, port, sessionId, getDefaultSessionOpts(),
				new DefaultSessionListener());

		if (status != Status.OK) {
			return;
		}

		LOG.debug("Successfully joined About bus session with sessionId {}", sessionId.value);
		
		AboutProxy aboutProxy = new AboutProxy(bus.getBusAttachment(), busName, sessionId.value);

		try {

			AboutObjectDescription aod[] = aboutProxy.getObjectDescription();

			if (aod != null) {
				for (AboutObjectDescription o : aod) {

					ProxyBusObject proxy = bus.getBusAttachment().getProxyBusObject(busName, o.path, sessionId.value,
							new Class<?>[] { AllSeenIntrospectable.class });

					SessionInfo sessionInfo = new SessionInfo();
					sessionInfo.sessionId = sessionId.value;
					sessionInfo.busName = busName;
					sessionInfo.namePrefix = o.interfaces[0];
					sessionInfo.port = port;
					sessionInfo.path = o.path;
					sessionInfo.sessionOpts = getDefaultSessionOpts();

					Globals.addSession(sessionInfo);

					AllSeenIntrospectable introspectable = proxy.getInterface(AllSeenIntrospectable.class);

					IntrospectResult introspectResult = introspector.doIntrospection(introspectable);

					discoveryService.handleAsync(introspectResult);

				}
			}

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

}
