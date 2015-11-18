package com.covisint.platform.gateway.repository.session;

import java.util.List;

public interface SessionRepository {

	AboutSession getAboutSessionById(int sessionId);

	void createAboutSession(AboutSession session);

	void deleteAboutSession(int sessionId);

	void clearDeviceSession(String deviceId);

	List<SessionEndpoint> getEndpointsByDevice(String deviceId);

	void clearAll();

}
