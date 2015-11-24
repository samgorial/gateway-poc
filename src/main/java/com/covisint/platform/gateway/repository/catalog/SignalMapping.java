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
@Table(name = "SIGNAL_MAPPING")
public class SignalMapping implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "PARENT_CATALOG_ITEM_ID", referencedColumnName = "ID", nullable = false)
	private CatalogItem parentCatalogItem;

	@Id
	@Column(name = "SIGNAL_NAME", nullable = false, length = 100)
	private String signalName;

	@Column(name = "EVENT_TEMPLATE_ID", nullable = false, length = 64)
	private String eventTemplateId;

	@OneToMany(fetch = FetchType.EAGER, mappedBy = "parentSignalMapping", cascade = CascadeType.ALL)
	private List<SignalArgMapping> args = new ArrayList<>();

	public CatalogItem getParentCatalogItem() {
		return parentCatalogItem;
	}

	public void setParentCatalogItem(CatalogItem parentCatalogItem) {
		this.parentCatalogItem = parentCatalogItem;
	}

	public String getSignalName() {
		return signalName;
	}

	public void setSignalName(String signalName) {
		this.signalName = signalName;
	}

	public String getEventTemplateId() {
		return eventTemplateId;
	}

	public void setEventTemplateId(String eventTemplateId) {
		this.eventTemplateId = eventTemplateId;
	}

	public List<SignalArgMapping> getArgs() {
		return args;
	}

	public void setArgs(List<SignalArgMapping> args) {
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