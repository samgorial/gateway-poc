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
@Table(name = "METHOD_ARG_MAPPING")
public class MethodArgMapping implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumns({
			@JoinColumn(name = "PARENT_CATALOG_ITEM_ID", referencedColumnName = "PARENT_CATALOG_ITEM_ID", nullable = false),
			@JoinColumn(name = "PARENT_METHOD_NAME", referencedColumnName = "METHOD_NAME", nullable = false) })
	private MethodMapping parentMethodMapping;

	@Id
	@Column(name = "ARG_NAME", nullable = false, length = 100)
	private String argName;

	@Column(name = "ARG_TYPE", nullable = false, length = 20)
	private String argType;

	@Column(name = "COMMAND_ARG_NAME", nullable = false, length = 100)
	private String commandArgName;

	public MethodMapping getParentMethodMapping() {
		return parentMethodMapping;
	}

	public void setParentMethodMapping(MethodMapping parentMethodMapping) {
		this.parentMethodMapping = parentMethodMapping;
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

	public String getCommandArgName() {
		return commandArgName;
	}

	public void setCommandArgName(String commandArgName) {
		this.commandArgName = commandArgName;
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