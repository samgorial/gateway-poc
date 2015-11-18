package com.covisint.platform.gateway.discovery;

import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import com.covisint.platform.device.core.device.Device;
import com.covisint.platform.gateway.domain.alljoyn.AJInterface;
import com.covisint.platform.gateway.repository.DeviceCatalogDao;
import com.covisint.platform.gateway.repository.DeviceProvisionerDao;
import com.covisint.platform.gateway.repository.InterfaceBlacklistedException;
import com.google.common.base.Stopwatch;

@Component
public class DiscoveryService {

	private static final Logger LOG = LoggerFactory.getLogger(DiscoveryService.class);

	@Value("${alljoyn.advertised_name_pfx}")
	private String advertisedNamePrefix;

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

			// bind methods

		} else {
			LOG.debug("Matched existing device {} to interface {}.  No further action to take.", device.getId(),
					intf.getName());
			known = true;
		}

		LOG.debug("Processing interface {} took {}", intf.getName(), clock);

		return new AsyncResult<Boolean>(known);
	}

}
