package com.covisint.platform.gateway.domain.alljoyn;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

abstract class AJBaseObject {

	public String toString() {
		return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
	}

	public boolean equals(Object that) {
		return EqualsBuilder.reflectionEquals(this, that);
	}

	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

}
