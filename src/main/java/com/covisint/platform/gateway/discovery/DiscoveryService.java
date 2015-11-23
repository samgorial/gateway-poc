package com.covisint.platform.gateway.discovery;

import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.Executors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.alljoyn.bus.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.covisint.platform.device.core.DataType;
import com.covisint.platform.device.core.commandtemplate.CommandTemplate;
import com.covisint.platform.device.core.device.Device;
import com.covisint.platform.device.core.devicetemplate.DeviceTemplate;
import com.covisint.platform.gateway.domain.alljoyn.AJInterface;
import com.covisint.platform.gateway.domain.alljoyn.AJMethod;
import com.covisint.platform.gateway.repository.catalog.ArgMapping;
import com.covisint.platform.gateway.repository.catalog.CatalogItem;
import com.covisint.platform.gateway.repository.catalog.CatalogRepository;
import com.covisint.platform.gateway.repository.catalog.MethodMapping;
import com.covisint.platform.gateway.repository.session.AboutSession;
import com.covisint.platform.gateway.repository.session.SessionEndpoint;
import com.covisint.platform.gateway.repository.session.SessionRepository;
import com.covisint.platform.gateway.util.AllJoynSupport;
import com.covisint.platform.gateway.util.CommandArgMatchProcessor;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

@Service
public class DiscoveryService {

	private static final Logger LOG = LoggerFactory.getLogger(DiscoveryService.class);

	@Autowired
	private CatalogRepository catalogRepository;

	@Autowired
	private SessionRepository sessionRepository;

	@Autowired
	private ProvisionerService provisionerService;

	@Async("listeningExecutor")
	public ListenableFuture<Optional<Device>> processInterface(AJInterface intf, AboutSession session, String path) {

		Stopwatch clock = Stopwatch.createStarted();
		String name = intf.getName();

		LOG.debug("Processing interface {}", name);

		if (catalogRepository.isBlacklisted(intf)) {
			LOG.debug("Interface {} is blacklisted.  Not continuing.", intf);
			return Futures.immediateFuture(Optional.<Device> absent());
		}

		CatalogItem catalogItem = catalogRepository.searchByInterface(intf.getName());

		String matchedDeviceTemplateId;

		if (catalogItem == null) {
			matchedDeviceTemplateId = provisionInterface(intf, name);
			// TODO bind methods?
		} else {
			String templateId = catalogItem.getDeviceTemplateId();
			LOG.debug("Catalog entry exists between device template {} and interface {}.", templateId, name);
			matchedDeviceTemplateId = templateId;
		}

		LOG.debug("Device template identified for interface {}; now searching for matching devices.", name);

		Device device = provisionerService.searchDevices(matchedDeviceTemplateId, intf.getAboutData());

		if (device == null) {
			LOG.debug("No device matched for interface {}.", name);
			device = provisionDevice(matchedDeviceTemplateId, intf.getAboutData());
		}

		LOG.debug("Creating session endpoint to map session, path, interface and device/device template.");

		SessionEndpoint endpoint = new SessionEndpoint();
		endpoint.setParentSession(session);
		endpoint.setIntf(intf.getName());
		endpoint.setPath(path);
		endpoint.setAssociatedDeviceTemplateId(matchedDeviceTemplateId);
		endpoint.setAssociatedDeviceId(device.getId());

		sessionRepository.addSessionEndpoint(session.getSessionId(), endpoint);

		LOG.info("Processing interface {} took {}", name, clock);

		return Futures.immediateFuture(Optional.of(device));
	}

	private Device provisionDevice(String deviceTemplateId, Map<String, Variant> aboutData) {

		LOG.info("Provisioning new device for template {}", deviceTemplateId);

		Device device = provisionerService.createDevice(deviceTemplateId, aboutData);

		LOG.info("Provisioned new device: \n{}", device);

		return device;
	}

	private String provisionInterface(AJInterface intf, String name) {

		LOG.info("Catalog entry not found for interface {}, so will provision now.", name);

		DeviceTemplate deviceTemplate = provisionerService.searchDeviceTemplates(intf);

		if (deviceTemplate == null) {
			LOG.debug("No cloud device templates matching interface {}.  Will provision one now.", name);
			deviceTemplate = provisionerService.createDeviceTemplate(intf);
			LOG.info("Provisioned device template for interface {}: \n{}", name, deviceTemplate);
		}

		LOG.debug("Created complete catalog entry for interface {}", name);

		CatalogItem catalogItem = new CatalogItem();
		catalogItem.setIface(name);
		catalogItem.setDeviceTemplateId(deviceTemplate.getId());
		catalogItem.setIntrospectionXml(marshalInterfaceXml(intf));

		addMethodMappings(catalogItem, intf, deviceTemplate);

		catalogRepository.createCatalogItem(catalogItem);

		return deviceTemplate.getId();
	}

	private void addMethodMappings(CatalogItem catalogItem, AJInterface intf, DeviceTemplate deviceTemplate) {

		if (intf.getMethods() == null) {
			return;
		}

		for (AJMethod method : intf.getMethods()) {

			MethodMapping methodMapping = null;

			for (CommandTemplate commandTmpl : deviceTemplate.getCommandTemplates()) {

				if (!commandTmpl.getName().equals(method.getName())) {
					continue;
				}

				methodMapping = new MethodMapping();
				methodMapping.setParentCatalogItem(catalogItem);
				methodMapping.setCommandTemplateId(commandTmpl.getId());
				methodMapping.setMethodName(method.getName());

				final MethodMapping mm = methodMapping;

				AllJoynSupport.matchCommandArgs(commandTmpl, method, new CommandArgMatchProcessor() {

					public void onMatch(String methodArgName, String commandArgName, String methodArgType,
							DataType commandArgType) {

						ArgMapping argMapping = new ArgMapping();
						argMapping.setArgName(methodArgName);
						argMapping.setArgType(methodArgType);
						argMapping.setCommandArgName(commandArgName);
						argMapping.setParentMethodMapping(mm);

						mm.getArgs().add(argMapping);
					}

				});

				break;

			}

			if (methodMapping == null) {
				LOG.debug("Could not establish mapping for method {}.  No suitable command available.",
						method.getName());
			} else {
				LOG.debug("Established mapping between command template id {} and AJ method {}",
						methodMapping.getCommandTemplateId(), method.getName());
				catalogItem.getMethodMappings().add(methodMapping);
			}

		}

		// TODO add signal mappings

	}

	// private void addArgMappings(MethodMapping methodMapping, AJMethod method,
	// CommandTemplate commandTmpl) {
	//
	// if(method.getArgs() == null) {
	// LOG.debug("Method {} declares no args.", method.getName());
	// return;
	// }
	//
	// for(AJArg arg : method.getArgs()) {
	//
	// for(CommandArg carg : commandTmpl.getArgs()) {
	//
	// if(arg.get)
	//
	// }
	//
	// }
	//
	// }

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

	@Bean
	private ListeningExecutorService listeningExecutor() {
		return MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(3));
	}

}
