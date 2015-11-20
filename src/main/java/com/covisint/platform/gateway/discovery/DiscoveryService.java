package com.covisint.platform.gateway.discovery;

import java.io.StringWriter;
import java.util.concurrent.Future;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import com.covisint.platform.device.core.devicetemplate.DeviceTemplate;
import com.covisint.platform.gateway.domain.alljoyn.AJInterface;
import com.covisint.platform.gateway.repository.catalog.CatalogItem;
import com.covisint.platform.gateway.repository.catalog.CatalogRepository;
import com.google.common.base.Stopwatch;

@Service
public class DiscoveryService {

	private static final Logger LOG = LoggerFactory.getLogger(DiscoveryService.class);

	@Autowired
	private CatalogRepository catalogRepository;

	@Autowired
	private ProvisionerService provisionerService;

	@Async
	public Future<Boolean> processInterface(AJInterface intf) {

		Stopwatch clock = Stopwatch.createStarted();
		String name = intf.getName();

		LOG.debug("Processing interface {}", name);

		if (catalogRepository.isBlacklisted(intf)) {
			LOG.debug("Interface {} is blacklisted.  Not continuing.");
			return new AsyncResult<>(true);
		}

		CatalogItem catalogItem = catalogRepository.searchByInterface(intf.getName());

		boolean known = false;

		if (catalogItem == null) {
			LOG.info("Catalog entry not found for interface {}, so will provision now.", name);

			DeviceTemplate deviceTemplate = provisionerService.searchDeviceTemplates(intf);

			if (deviceTemplate == null) {
				LOG.debug("No cloud device templates matching interface {}.  Will provision one now.", name);
				deviceTemplate = provisionerService.createDeviceTemplate(intf);
				LOG.info("Provisioned device template for interface {}: \n{}", name, deviceTemplate);
			}

			catalogItem = new CatalogItem();
			catalogItem.setIface(name);
			catalogItem.setDeviceTemplateId(deviceTemplate.getId());
			catalogItem.setIntrospectionXml(marshalInterfaceXml(intf));

			catalogRepository.createCatalogItem(catalogItem);

			// TODO bind methods?

		} else {
			LOG.debug("Catalog entry exists between device template {} and interface {}.  No further action to take.",
					catalogItem.getDeviceTemplateId(), name);
			known = true;
		}

		LOG.debug("Processing interface {} took {}", name, clock);

		return new AsyncResult<Boolean>(known);
	}

	private static String marshalInterfaceXml(AJInterface intf) {

		JAXBContext jaxb;

		try {
			jaxb = JAXBContext.newInstance(AJInterface.class);

			Marshaller marshaller = jaxb.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

			StringWriter writer = new StringWriter();

			marshaller.marshal(intf, writer);

			return writer.toString();

		} catch (JAXBException e) {
			throw new RuntimeException("Error occurred while marshalling AllJoyn interface", e);
		}

	}

}
