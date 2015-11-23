package com.covisint.platform.gateway.repository.catalog;

import com.covisint.platform.gateway.domain.AJInterface;

public interface CatalogRepository {

	CatalogItem searchByInterface(String interfaceName);

	CatalogItem createCatalogItem(CatalogItem catalogItem);

	void addToBlacklist(String interfaceName, String reason);

	boolean isBlacklisted(AJInterface intf);

}
