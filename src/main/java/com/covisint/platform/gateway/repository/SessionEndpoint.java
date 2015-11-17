package com.covisint.platform.gateway.repository;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

@Entity
@Table(name = "SESSION_ENDPOINT")
@NamedQueries({
		@NamedQuery(name = "SessionEndpoint.getByInterface", query = "FROM SessionEndpoint WHERE intf = :i AND parentSession.sessionType = :t"),
		@NamedQuery(name = "SessionEndpoint.getByDeviceId", query = "FROM SessionEndpoint WHERE associatedDeviceId = :id") })
public class SessionEndpoint implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "PARENT_SESSION_ID", nullable = false)
	private SessionInfo parentSession;

	@Id
	@Column(name = "INTERFACE")
	private String intf;

	@Id
	@Column(name = "PATH")
	private String path;

	@Column(name = "DEVICE_ID")
	private String associatedDeviceId;

	public SessionInfo getParentSession() {
		return parentSession;
	}

	public void setParentSession(SessionInfo parentSession) {
		this.parentSession = parentSession;
	}

	public String getIntf() {
		return intf;
	}

	public void setIntf(String intf) {
		this.intf = intf;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getAssociatedDeviceId() {
		return associatedDeviceId;
	}

	public void setAssociatedDeviceId(String associatedDeviceId) {
		this.associatedDeviceId = associatedDeviceId;
	}

	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}

	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

}
