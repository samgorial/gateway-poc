package com.covisint.platform.gateway.repository.catalog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

@Entity
@Table(name = "METHOD_MAPPING")
public class MethodMapping implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "PARENT_CATALOG_ITEM_ID", referencedColumnName = "ID", nullable = false)
	private CatalogItem parentCatalogItem;

	@Id
	@Column(name = "METHOD_NAME", nullable = false, length = 100)
	private String methodName;

	@Column(name = "COMMAND_TEMPLATE_ID", nullable = false, length = 64)
	private String commandTemplateId;

	@OneToMany(fetch = FetchType.EAGER, mappedBy = "parentMethodMapping", cascade = CascadeType.ALL)
	private List<MethodArgMapping> args = new ArrayList<>();

	public CatalogItem getParentCatalogItem() {
		return parentCatalogItem;
	}

	public void setParentCatalogItem(CatalogItem parentCatalogItem) {
		this.parentCatalogItem = parentCatalogItem;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public String getCommandTemplateId() {
		return commandTemplateId;
	}

	public void setCommandTemplateId(String commandTemplateId) {
		this.commandTemplateId = commandTemplateId;
	}

	public List<MethodArgMapping> getArgs() {
		return args;
	}

	public void setArgs(List<MethodArgMapping> args) {
		this.args = args;
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