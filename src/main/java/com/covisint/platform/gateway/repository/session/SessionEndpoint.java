package com.covisint.platform.gateway.repository.session;

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
		@NamedQuery(name = "SessionEndpoint.getByDeviceId", query = "FROM SessionEndpoint WHERE deviceId = :id") })
public class SessionEndpoint implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "PARENT_SESSION_ID", nullable = false)
	private AboutSession parentSession;

	@Id
	@Column(name = "INTERFACE")
	private String intf;

	@Id
	@Column(name = "PATH")
	private String path;

	@Column(name = "DEVICE_TEMPLATE_ID")
	private String deviceTemplateId;

	@Column(name = "DEVICE_ID")
	private String deviceId;

	public AboutSession getParentSession() {
		return parentSession;
	}

	public void setParentSession(AboutSession parentSession) {
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

	public String getAssociatedDeviceTemplateId() {
		return deviceTemplateId;
	}

	public void setAssociatedDeviceTemplateId(String deviceTemplateId) {
		this.deviceTemplateId = deviceTemplateId;
	}

	public String getAssociatedDeviceId() {
		return deviceId;
	}

	public void setAssociatedDeviceId(String deviceId) {
		this.deviceId = deviceId;
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
