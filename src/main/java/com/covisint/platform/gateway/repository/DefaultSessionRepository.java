package com.covisint.platform.gateway.repository;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.covisint.platform.gateway.repository.SessionInfo.SessionType;

@Repository
public class DefaultSessionRepository implements SessionRepository {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultSessionRepository.class);

	@PersistenceContext
	private EntityManager em;

	@Transactional
	public void createSession(SessionInfo info) {

		if (getSessionById(info.getSessionId()) != null) {
			throw new IllegalStateException("Session " + info.getSessionId() + " already exists.");
		}

		em.persist(info);
		LOG.debug("Persisted session {}", info);
	}

	@Transactional
	public void updateSession(SessionInfo info) {

		if (getSessionById(info.getSessionId()) == null) {
			throw new IllegalStateException("Session " + info.getSessionId() + " does not exist.");
		}

		em.merge(info);
		LOG.debug("Updated session {}", info.getSessionId());
	}

	@Transactional
	public void deleteSession(int sessionId) {
		SessionInfo existing = getSessionById(sessionId);
		if (existing != null) {
			em.remove(existing);
		} else {
			LOG.warn("Could not find session by id {} to delete.", sessionId);
		}
	}

	@Transactional(readOnly = true)
	public SessionInfo getSessionById(int sessionId) {

		SessionInfo session = em.find(SessionInfo.class, sessionId);

		if (session == null) {
			LOG.debug("Could not find session {}", sessionId);
			return null;
		} else {
			return session;
		}
	}

	public List<SessionEndpoint> getEndpointsByInterface(String intf, SessionType type) {
		TypedQuery<SessionEndpoint> query = em.createNamedQuery("SessionEndpoint.getByInterface",
				SessionEndpoint.class);

		query.setParameter("i", intf);
		query.setParameter("t", type);

		List<SessionEndpoint> endpoints = query.getResultList();

		if (endpoints == null || endpoints.isEmpty()) {
			LOG.warn("Could not find any registered endpoints for interface {}", intf);
			return new ArrayList<>();
		}

		return endpoints;
	}

	@Transactional
	public void setDeviceSession(String deviceId, int sessionId, String intf) {
		SessionInfo session = getSessionById(sessionId);

		if (session == null) {
			throw new IllegalArgumentException("No session exists by id " + sessionId);
		}

		boolean updated = false;
		List<SessionEndpoint> endpoints = getEndpointsByInterface(intf, SessionType.STANDARD);

		for (SessionEndpoint endpoint : endpoints) {
			if (endpoint.getParentSession().getSessionId() != sessionId) {
				continue;
			}

			endpoint.setAssociatedDeviceId(deviceId);
			em.merge(endpoint);
			updated = true;
			break;
		}

		if (!updated) {
			throw new IllegalStateException("No interface " + intf + " is mapped for session " + sessionId);
		}

		LOG.debug("Associated device {} to session {}", deviceId, sessionId);
	}

	@Transactional
	public void clearDeviceSession(String deviceId) {

		List<SessionEndpoint> endpoints = getEndpointsByDevice(deviceId);

		for (SessionEndpoint endpoint : endpoints) {
			endpoint.setAssociatedDeviceId(null);
			em.merge(endpoint);
		}

		LOG.debug("Cleared all session mappings for device {}", deviceId);
	}

	public List<SessionEndpoint> getEndpointsByDevice(String deviceId) {

		TypedQuery<SessionEndpoint> query = em.createNamedQuery("SessionEndpoint.getByDeviceId", SessionEndpoint.class);

		query.setParameter("id", deviceId);

		List<SessionEndpoint> endpoints = query.getResultList();

		if (endpoints == null || endpoints.isEmpty()) {
			LOG.warn("Could not find any registered endpoints for device {}", deviceId);
			return new ArrayList<>();
		}

		return endpoints;
	}

}