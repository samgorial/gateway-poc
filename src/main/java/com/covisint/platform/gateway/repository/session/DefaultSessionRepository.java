package com.covisint.platform.gateway.repository.session;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class DefaultSessionRepository implements SessionRepository {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultSessionRepository.class);

	@PersistenceContext
	private EntityManager em;

	@Transactional
	public void createAboutSession(AboutSession session) {

		if (getAboutSessionById(session.getSessionId()) != null) {
			throw new IllegalStateException("Session " + session.getSessionId() + " already exists.");
		}

		em.persist(session);
		LOG.debug("Persisted session {}", session);
	}

	@Transactional
	public void deleteAboutSession(int sessionId) {
		AboutSession existing = getAboutSessionById(sessionId);
		if (existing != null) {
			em.remove(existing);
		} else {
			LOG.warn("Could not find session by id {} to delete.", sessionId);
		}
	}

	@Transactional(readOnly = true)
	public AboutSession getAboutSessionById(int sessionId) {

		AboutSession session = em.find(AboutSession.class, sessionId);

		if (session == null) {
			LOG.debug("Could not find session {}", sessionId);
			return null;
		} else {
			return session;
		}
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

	@Transactional(readOnly = true)
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

	@Transactional
	public void clearAll() {
		em.createQuery("DELETE FROM AboutSession").executeUpdate();

		LOG.debug("Cleared all About sessions including associated endpoints.");
	}

}