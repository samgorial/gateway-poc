package com.covisint.platform.gateway.domain;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "annotation")
public class AJAnnotation extends AJBaseObject {

	private String name;

	private String value;

	public String getName() {
		return name;
	}

	@XmlAttribute(name = "name")
	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	@XmlAttribute(name = "value")
	public void setValue(String value) {
		this.value = value;
	}

}
