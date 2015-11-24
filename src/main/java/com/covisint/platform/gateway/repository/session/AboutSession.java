package com.covisint.platform.gateway.repository.session;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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
@Table(name = "ABOUT_SESSION")
public class AboutSession implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "SESSION_ID")
	private int sessionId;

	@Column(name = "BUS_NAME", nullable = false, length = 50)
	private String busName;

	@Column(name = "TRANSPORT")
	private short transport;

	@Column(name = "PORT")
	private short port;

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

	public short getPort() {
		return port;
	}

	public void setPort(short port) {
		this.port = port;
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

}