package com.covisint.platform.gateway.repository.catalog;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

@Entity
@Table(name = "SIGNAL_ARG_MAPPING")
public class SignalArgMapping implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumns({
			@JoinColumn(name = "PARENT_CATALOG_ITEM_ID", referencedColumnName = "PARENT_CATALOG_ITEM_ID", nullable = false),
			@JoinColumn(name = "PARENT_SIGNAL_NAME", referencedColumnName = "SIGNAL_NAME", nullable = false) })
	private SignalMapping parentSignalMapping;

	@Id
	@Column(name = "ARG_NAME")
	private String argName;

	@Column(name = "ARG_TYPE")
	private String argType;

	@Column(name = "EVENT_FIELD_NAME")
	private String eventFieldName;

	public SignalMapping getParentSignalMapping() {
		return parentSignalMapping;
	}

	public void setParentSignalMapping(SignalMapping parentSignalMapping) {
		this.parentSignalMapping = parentSignalMapping;
	}

	public String getArgName() {
		return argName;
	}

	public void setArgName(String argName) {
		this.argName = argName;
	}

	public String getArgType() {
		return argType;
	}

	public void setArgType(String argType) {
		this.argType = argType;
	}

	public String getEventFieldName() {
		return eventFieldName;
	}

	public void setEventFieldName(String eventFieldName) {
		this.eventFieldName = eventFieldName;
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