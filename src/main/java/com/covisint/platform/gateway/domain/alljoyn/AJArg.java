package com.covisint.platform.gateway.domain.alljoyn;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "arg")
public class AJArg extends AJBaseObject {

	private String name;

	private String type;

	private String direction;

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

	public String getDirection() {
		return direction;
	}

	@XmlAttribute(name = "direction")
	public void setDirection(String direction) {
		this.direction = direction;
	}

}
