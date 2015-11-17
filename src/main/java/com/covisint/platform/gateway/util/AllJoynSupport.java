package com.covisint.platform.gateway.util;

import org.alljoyn.bus.SessionOpts;

public class AllJoynSupport {

	public static final SessionOpts getDefaultSessionOpts() {
		SessionOpts opts = new SessionOpts();
		opts.traffic = SessionOpts.TRAFFIC_MESSAGES;
		opts.isMultipoint = true;
		opts.proximity = SessionOpts.PROXIMITY_ANY;
		opts.transports = SessionOpts.TRANSPORT_IP;
		return opts;
	}
	
}
