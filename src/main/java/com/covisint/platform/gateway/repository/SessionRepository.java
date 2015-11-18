package com.covisint.platform.gateway.repository;

import java.util.List;

import com.covisint.platform.gateway.repository.SessionInfo.SessionType;

public interface SessionRepository {

	SessionInfo getSessionById(int sessionId);

	void createSession(SessionInfo info);

	void updateSession(SessionInfo info);

	void deleteSession(int sessionId);

	void setDeviceSession(String deviceId, int sessionId, String intf);

	void clearDeviceSession(String deviceId);

	List<SessionEndpoint> getEndpointsByDevice(String deviceId);

	List<SessionEndpoint> getEndpointsByInterface(String intf, SessionType type);

}
