package com.covisint.platform.gateway.repository.catalog;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.covisint.platform.gateway.domain.alljoyn.AJInterface;

@Component
public class DefaultCatalogRepository implements CatalogRepository {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultCatalogRepository.class);

	@PersistenceContext
	private EntityManager em;

	@Transactional(readOnly = true)
	public CatalogItem searchByInterface(String interfaceName) {

		LOG.debug("Searching for catalog entries for interface {}", interfaceName);

		TypedQuery<CatalogItem> query = em.createNamedQuery("CatalogItem.getByInterface", CatalogItem.class);
		query.setParameter("i", interfaceName);

		try {
			CatalogItem match = query.getSingleResult();

			LOG.debug("Found matching catalog entry: {}", match.getId());

			return match;

		} catch (NoResultException e) {
			LOG.debug("Did not find any catalog entry for interface {}", interfaceName);
			return null;
		} catch (NonUniqueResultException e) {
			throw new RuntimeException("Did not expect multiple catalog results for interface " + interfaceName, e);
		}

	}

	@Transactional
	public CatalogItem createCatalogItem(CatalogItem catalogItem) {

		LOG.debug("Creating catalog item {}", catalogItem);

		em.persist(catalogItem);

		LOG.debug("Successfully created catalog item.");

		return catalogItem;
	}

	@Transactional
	public void addToBlacklist(String interfaceName, String reason) {

		try {
			if (em.find(BlacklistedInterface.class, interfaceName) != null) {
				LOG.debug("Interface {} already blacklisted.", interfaceName);
				return;
			}
		} catch (NoResultException e) {
			// This is ok.
		}

		BlacklistedInterface bl = new BlacklistedInterface();
		bl.setIface(interfaceName);
		bl.setReason(reason);
		bl.setBlacklistInstant(System.currentTimeMillis());

		em.persist(bl);

		LOG.debug("Successfully blacklisted {} with reason '{}'", interfaceName, reason);
	}

	@Transactional(readOnly = true)
	public boolean isBlacklisted(AJInterface intf) {
		return em.find(BlacklistedInterface.class, intf.getName()) != null;
	}

}
