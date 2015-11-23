package com.covisint.platform.gateway.domain;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "method")
public class AJMethod extends AJBaseObject {

	private String name;

	private List<AJArg> args;
	
	private List<AJAnnotation> annotations;

	@XmlAttribute(name = "name")
	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public List<AJArg> getArgs() {
		return args;
	}

	@XmlElement(name = "arg")
	public void setArgs(List<AJArg> args) {
		this.args = args;
	}
	
	public List<AJAnnotation> getAnnotations() {
		return annotations;
	}
	
	@XmlElement(name = "annotation")
	public void setAnnotations(List<AJAnnotation> annotations) {
		this.annotations = annotations;
	}

}
