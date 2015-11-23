package com.covisint.platform.gateway.discovery;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.covisint.platform.gateway.domain.AJInterface;

@XmlRootElement(name = "node")
public class IntrospectResult {

	private List<AJInterface> interfaces;

	@XmlElement(name = "interface")
	public void setInterfaces(List<AJInterface> interfaces) {
		this.interfaces = interfaces;
	}

	public List<AJInterface> getInterfaces() {
		return interfaces;
	}

	public String toString() {
		return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
	}

}
