package com.covisint.platform.gateway.repository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.alljoyn.bus.SessionOpts;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

@Entity
@Table(name = "SESSION_INFO")
public class SessionInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "SESSION_ID")
	private int sessionId;

	@Column(name = "BUS_NAME")
	private String busName;

	@Column(name = "TRANSPORT")
	private short transport;

	@Column(name = "NAME_PREFIX")
	private String namePrefix;

	@Column(name = "PORT")
	private short port;

	@Column(name = "TYPE")
	@Enumerated(EnumType.STRING)
	private SessionType sessionType;

	@OneToMany(fetch = FetchType.EAGER, mappedBy = "parentSession", cascade = CascadeType.ALL)
	private List<SessionEndpoint> endpoints = new ArrayList<>();

	@Transient
	private SessionOpts sessionOpts;

	public int getSessionId() {
		return sessionId;
	}

	public void setSessionId(int sessionId) {
		this.sessionId = sessionId;
	}

	public String getBusName() {
		return busName;
	}

	public void setBusName(String busName) {
		this.busName = busName;
	}

	public short getTransport() {
		return transport;
	}

	public void setTransport(short transport) {
		this.transport = transport;
	}

	public String getNamePrefix() {
		return namePrefix;
	}

	public void setNamePrefix(String namePrefix) {
		this.namePrefix = namePrefix;
	}

	public short getPort() {
		return port;
	}

	public void setPort(short port) {
		this.port = port;
	}

	public SessionType getSessionType() {
		return sessionType;
	}

	public void setSessionType(SessionType sessionType) {
		this.sessionType = sessionType;
	}

	public List<SessionEndpoint> getEndpoints() {
		return endpoints;
	}

	public void setEndpoints(List<SessionEndpoint> endpoints) {
		this.endpoints = endpoints;
	}

	public SessionOpts getSessionOpts() {
		return sessionOpts;
	}

	public void setSessionOpts(SessionOpts sessionOpts) {
		this.sessionOpts = sessionOpts;
	}

	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}

	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}

	public enum SessionType {
		ABOUT, STANDARD;
	}

}