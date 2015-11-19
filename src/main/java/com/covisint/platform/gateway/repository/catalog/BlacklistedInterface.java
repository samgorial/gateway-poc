package com.covisint.platform.gateway.repository.catalog;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

@Entity
@Table(name = "INTERFACE_BLACKLIST")
public class BlacklistedInterface implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "INTERFACE")
	private String iface;

	@Column(name = "REASON")
	private String reason;

	@Column(name = "BLACKLIST_INSTANT")
	private Long blacklistInstant;

	public String getIface() {
		return iface;
	}

	public void setIface(String iface) {
		this.iface = iface;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public Long getBlacklistInstant() {
		return blacklistInstant;
	}

	public void setBlacklistInstant(Long blacklistInstant) {
		this.blacklistInstant = blacklistInstant;
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