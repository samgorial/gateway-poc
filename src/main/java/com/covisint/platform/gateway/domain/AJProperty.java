package com.covisint.platform.gateway.domain;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "property")
public class AJProperty extends AJBaseObject {

	private String name;

	private String type;

	private String value;

	private String access;

	public String getName() {
		return name;
	}

	@XmlAttribute(name = "name")
	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	@XmlAttribute(name = "type")
	public void setType(String type) {
		this.type = type;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getAccess() {
		return access;
	}

	@XmlAttribute(name = "access")
	public void setAccess(String access) {
		this.access = access;
	}

}
