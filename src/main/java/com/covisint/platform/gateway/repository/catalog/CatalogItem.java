package com.covisint.platform.gateway.repository.catalog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

@Entity
@Table(name = "CATALOG_ITEM", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "INTERFACE", "DEVICE_TEMPLATE_ID" }) })
@NamedQuery(name = "CatalogItem.getByInterface", query = "FROM CatalogItem WHERE iface = :i")
public class CatalogItem implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "ID")
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id;

	@Column(name = "INTERFACE")
	private String iface;

	@Column(name = "DEVICE_TEMPLATE_ID")
	private String deviceTemplateId;

	@Column(name = "INTROSPECTION_XML")
	@Lob
	private String introspectionXml;

	@OneToMany(fetch = FetchType.EAGER, mappedBy = "parentCatalogItem", cascade = CascadeType.ALL)
	private List<MethodMapping> methodMappings = new ArrayList<>();

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getIface() {
		return iface;
	}

	public void setIface(String iface) {
		this.iface = iface;
	}

	public String getDeviceTemplateId() {
		return deviceTemplateId;
	}

	public void setDeviceTemplateId(String deviceTemplateId) {
		this.deviceTemplateId = deviceTemplateId;
	}

	public String getIntrospectionXml() {
		return introspectionXml;
	}

	public void setIntrospectionXml(String introspectionXml) {
		this.introspectionXml = introspectionXml;
	}

	public List<MethodMapping> getMethodMappings() {
		return methodMappings;
	}

	public void setMethodMappings(List<MethodMapping> methodMappings) {
		this.methodMappings = methodMappings;
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