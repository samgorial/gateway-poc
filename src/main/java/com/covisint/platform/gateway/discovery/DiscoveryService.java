package com.covisint.platform.gateway.discovery;

import java.util.concurrent.Future;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import com.covisint.platform.device.core.device.Device;
import com.covisint.platform.gateway.domain.alljoyn.AJInterface;
import com.covisint.platform.gateway.store.DeviceCatalogDao;
import com.covisint.platform.gateway.store.DeviceProvisionerDao;
import com.covisint.platform.gateway.store.InterfaceBlacklistedException;
import com.google.common.base.Stopwatch;

import mock.PiSignalHandler;

@Component
public class DiscoveryService {

	private static final Logger LOG = LoggerFactory.getLogger(DiscoveryService.class);

	@Value("${advertised.name.pfx}")
	private String advertisedNamePrefix;

	@Autowired
	private BusAttachment bus;

	@Autowired
	private DeviceCatalogDao catalog;

	@Autowired
	private DeviceProvisionerDao provisioner;

	public void handleAsync(final IntrospectResult metadata) {

		LOG.debug("Asynchronously processing AJ metadata containing {} interfaces.", metadata.getInterfaces().size());

		for (final AJInterface intf : metadata.getInterfaces()) {
			processInterface(intf);
		}

	}

	@Async
	private Future<Boolean> processInterface(AJInterface intf) {

		Stopwatch clock = Stopwatch.createStarted();

		LOG.debug("Processing interface {}", intf.getName());

		Device device = catalog.searchByInterface(intf);

		boolean known = false;

		if (device == null) {
			LOG.info("No match found in device catalog for interface {}.  Will provision now.", intf.getName());

			Device newDevice;

			try {
				newDevice = provisioner.provisionNewDevice(intf);
			} catch (InterfaceBlacklistedException e) {
				LOG.warn("Given interface is blacklisted, will not provision: {}", intf.getName());
				return new AsyncResult<>(true);
			}

			LOG.debug("Provisioned new device: \n{}", newDevice);

			registerSignalHandler(new PiSignalHandler()); // TODO FIXME
															// hardcoded for now

			// bind methods

		} else {
			LOG.debug("Matched existing device {} to interface {}.  No further action to take.", device.getId(),
					intf.getName());
			known = true;
		}

		LOG.debug("Processing interface {} took {}", intf.getName(), clock);

		return new AsyncResult<Boolean>(known);
	}

	private void registerSignalHandler(Object signalHandler) {

		Status status = bus.registerSignalHandlers(signalHandler);

		if (status != Status.OK) {
			LOG.error("Could not register signal handler with bus! {}", status);
			return;
		}

		status = bus.findAdvertisedName(advertisedNamePrefix);

		if (status != Status.OK) {
			LOG.error("Could not find advertised name! {}", status);
			return;
		}

	}

}
