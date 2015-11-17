package com.covisint.platform.gateway.session;

import org.alljoyn.bus.SessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.covisint.platform.gateway.repository.SessionRepository;

@Component
public class DefaultSessionListener extends SessionListener {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultSessionListener.class);

	@Autowired
	private SessionRepository sessionRepository;

	public void sessionLost(int sessionId, int reason) {
		super.sessionLost(sessionId, reason);

		LOG.debug("Lost session {} due to {}", sessionId, reason);

		sessionRepository.deleteSession(sessionId);
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
