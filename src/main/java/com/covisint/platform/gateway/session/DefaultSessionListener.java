package com.covisint.platform.gateway.session;

import org.alljoyn.bus.SessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mock.Globals;

public class DefaultSessionListener extends SessionListener {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultSessionListener.class);
	
	public void sessionLost(int sessionId, int reason) {
		super.sessionLost(sessionId, reason);
			
		LOG.debug("Lost session {} due to {}", sessionId, reason);
		
		Globals.removeSession(sessionId);
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
