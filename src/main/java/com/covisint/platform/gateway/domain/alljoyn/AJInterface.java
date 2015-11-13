package com.covisint.platform.gateway.domain.alljoyn;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "interface")
public class AJInterface extends AJBaseObject {

	private String name;

	private List<AJProperty> properties;

	private List<AJMethod> methods;

	private List<AJSignal> signals;

	@XmlAttribute(name = "name")
	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public List<AJProperty> getProperties() {
		return properties;
	}

	@XmlElement(name = "property")
	public void setProperties(List<AJProperty> properties) {
		this.properties = properties;
	}

	public List<AJMethod> getMethods() {
		return methods;
	}

	@XmlElement(name = "method")
	public void setMethods(List<AJMethod> methods) {
		this.methods = methods;
	}

	public List<AJSignal> getSignals() {
		return signals;
	}

	@XmlElement(name = "signal")
	public void setSignals(List<AJSignal> signals) {
		this.signals = signals;
	}

}
