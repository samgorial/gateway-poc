package com.covisint.platform.gateway.discovery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.covisint.core.http.service.core.InternationalString;
import com.covisint.core.http.service.core.Page;
import com.covisint.core.http.service.core.Resource;
import com.covisint.core.http.service.core.ServiceException;
import com.covisint.platform.device.client.FetchOpts;
import com.covisint.platform.device.client.attributetype.AttributeTypeSDK.AttributeTypeClient;
import com.covisint.platform.device.client.attributetype.AttributeTypeSDK.AttributeTypeClient.AttributeTypeFilterSpec;
import com.covisint.platform.device.client.commandtemplate.CommandTemplateSDK.CommandTemplateClient;
import com.covisint.platform.device.client.commandtemplate.CommandTemplateSDK.CommandTemplateClient.CommandTemplateFilterSpec;
import com.covisint.platform.device.client.device.DeviceSDK.DeviceClient;
import com.covisint.platform.device.client.device.DeviceSDK.DeviceClient.DeviceFilterSpec;
import com.covisint.platform.device.client.devicetemplate.DeviceTemplateSDK.DeviceTemplateClient;
import com.covisint.platform.device.client.devicetemplate.DeviceTemplateSDK.DeviceTemplateClient.DeviceTemplateFilterSpec;
import com.covisint.platform.device.client.eventtemplate.EventTemplateSDK.EventTemplateClient;
import com.covisint.platform.device.client.eventtemplate.EventTemplateSDK.EventTemplateClient.EventTemplateFilterSpec;
import com.covisint.platform.device.core.DataType;
import com.covisint.platform.device.core.attributetype.AttributeType;
import com.covisint.platform.device.core.commandtemplate.CommandArg;
import com.covisint.platform.device.core.commandtemplate.CommandTemplate;
import com.covisint.platform.device.core.device.Device;
import com.covisint.platform.device.core.devicetemplate.DeviceTemplate;
import com.covisint.platform.device.core.eventtemplate.EventField;
import com.covisint.platform.device.core.eventtemplate.EventTemplate;
import com.covisint.platform.eventSource.client.eventSource.EventSourceSDK.EventSourceClient;
import com.covisint.platform.eventSource.core.eventSource.EventSource;
import com.covisint.platform.gateway.domain.AJAnnotation;
import com.covisint.platform.gateway.domain.AJArg;
import com.covisint.platform.gateway.domain.AJInterface;
import com.covisint.platform.gateway.domain.AJMethod;
import com.covisint.platform.gateway.domain.AJProperty;
import com.covisint.platform.gateway.domain.AJSignal;
import com.covisint.platform.gateway.util.AllJoynSupport;
import com.covisint.platform.messaging.stream.client.sdk.StreamDeviceSDK.StreamDeviceClient;
import com.covisint.platform.messaging.stream.core.stream.device.StreamDevice;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

@Component
public class DefaultProvisionerService implements ProvisionerService {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultProvisionerService.class);

	@Value("${agent.name}")
	private String agentName;

	@Value("${agent.realm}")
	private String realm;

	@Value("${agent.stream_id}")
	private String streamId;

	@Value("${agent.event_source_id}")
	private String eventSourceId;

	@Value("${alljoyn.property_match_rating_threshold}")
	private double propertyMatchRatingThreshold;

	@Value("${alljoyn.signal_match_rating_threshold}")
	private double signalMatchRatingThreshold;

	@Value("${alljoyn.method_match_rating_threshold}")
	private double methodMatchRatingThreshold;

	@Autowired
	private AttributeTypeClient attributeTypeClient;

	@Autowired
	private CommandTemplateClient commandTemplateClient;

	@Autowired
	private EventTemplateClient eventTemplateClient;

	@Autowired
	private DeviceTemplateClient deviceTemplateClient;

	@Autowired
	private DeviceClient deviceClient;

	@Autowired
	private EventSourceClient eventSourceClient;

	@Autowired
	private StreamDeviceClient streamDeviceClient;

	public DeviceTemplate searchDeviceTemplates(AJInterface intf) {

		LOG.debug("About to search for device templates matching interface {}", intf.getName());

		DeviceTemplateFilterSpec filter = new DeviceTemplateFilterSpec();

		/*
		 * Search for all active templates. TODO need to narrow this down
		 * somehow.
		 */
		filter.setActive(true);

		List<DeviceTemplate> allTemplates = deviceTemplateClient.search(filter, Page.ALL).checkedGet();

		// Filter all templates down using the matcher.
		List<DeviceTemplate> filtered = FluentIterable.from(allTemplates).filter(new DeviceTemplateMatcher(intf))
				.toList();

		if (filtered.isEmpty()) {
			LOG.debug("Could not match any templates on interface {}", intf.getName());
			return null;
		} else if (filtered.size() > 1) {
			LOG.warn(
					"Problem, there are multiple ({}) matched device templates for interface {}, only expected 1.  Using the first one.",
					filtered.size(), intf.getName());
		}

		// TODO how to handle multiple template matches better?
		return fetchFullDeviceTemplate(filtered.get(0).getId());
	}

	public DeviceTemplate createDeviceTemplate(AJInterface intf) {

		LOG.info("Creating new device template for interface {} with details: \n{}", intf.getName(), intf);

		// Create attribute types, command and event templates based on
		// AJInterface
		List<AttributeType> attrTypes = createAttributeTypes(intf.getProperties(), intf.getAboutData());
		List<CommandTemplate> commandTemplates = createCommandTemplates(intf.getMethods());
		List<EventTemplate> eventTemplates = createEventTemplates(intf.getSignals());

		// Just to be safe, check for an existing template with the same DNA.
		DeviceTemplateFilterSpec filter = new DeviceTemplateFilterSpec();
		filter.setActive(true);
		filter.setAttributeTypeIds(FluentIterable.from(attrTypes).transform(IdGetter.INSTANCE).toSet());
		filter.setCommandTemplateIds(FluentIterable.from(commandTemplates).transform(IdGetter.INSTANCE).toSet());
		filter.setEventTemplateIds(FluentIterable.from(eventTemplates).transform(IdGetter.INSTANCE).toSet());

		List<DeviceTemplate> existing = deviceTemplateClient.search(filter, Page.ALL).checkedGet();

		if (!existing.isEmpty()) {
			// If we already found one, just return it.
			LOG.warn("Already found device template containing attribute types, "
					+ "events and commands specified in interface {}", intf.getName());
			return fetchFullDeviceTemplate(existing.get(0).getId());
		}

		// Create brand new device template.
		DeviceTemplate template = new DeviceTemplate();

		template.setRealm(realm);
		template.setName(new InternationalString("en", intf.getName()));
		template.setDescription(new InternationalString("en", "Created by " + agentName));
		template.setCreator(agentName);
		template.setCreatorApplicationId(agentName);
		template.setCreationInstant(System.currentTimeMillis());

		template.setAttributeTypes(attrTypes);
		template.setCommandTemplates(commandTemplates);
		template.setEventTemplates(eventTemplates);

		DeviceTemplate created = deviceTemplateClient.add(template).checkedGet();

		// Activate and tag it.
		deviceTemplateClient.activateDeviceTemplate(created.getId()).checkedGet();
		deviceTemplateClient.tagDeviceTemplate(created.getId(), "Created by " + agentName).checkedGet();

		LOG.debug("Created and activated device template successfully with id {}", created.getId());

		DeviceTemplate retrieved = fetchFullDeviceTemplate(created.getId());

		LOG.debug("Returning full device template resource: \n{}", retrieved);

		return retrieved;
	}

	public Device createDevice(String deviceTemplateId, Map<String, Variant> aboutData) {

		LOG.debug("Creating new device based on template {}", deviceTemplateId);

		// Create device instance
		Device device = deviceClient.createDeviceFromTemplate(deviceTemplateId).checkedGet();

		// Activate it
		deviceClient.activateDevice(device.getId());
		deviceClient.tagDevice(device.getId(), "Created by " + agentName).checkedGet();

		LOG.debug("Created, activated and tagged device with id {}", device.getId());

		// Now add this device to existing stream.
		addDeviceToStream(device);

		// Append all new event sources for this device.
		addNewEventSources(device);

		FetchOpts fetchWithAttrTypes = new FetchOpts().embedAttributeTypes();

		// Request newly created device along with all attribute type data
		// nested in it
		device = deviceClient.get(device.getId(), fetchWithAttrTypes).checkedGet();

		if (aboutData != null) {

			for (AttributeType attrType : device.getStandardAttributeTypes()) {

				String attrTypeId = attrType.getId();
				String attrTypeName = attrType.getName();

				Variant value = aboutData.get(attrTypeName);

				if (value == null) {
					LOG.debug(
							"No {} found for attribute type {} of device {}. "
									+ "This attribute may not be an 'AboutData' field.",
							Variant.class.getName(), attrTypeName, device.getId());
					continue;
				}

				try {
					if (attrTypeName.equals("AppId")) {
						String appId = getAppId(value);
						device.setStandardAttributeValue(attrTypeId, appId);

						// Also need to tag with the AppId (it's special)
						String tag = attrTypeName + ":" + appId;
						deviceClient.tagDevice(device.getId(), tag).checkedGet();
						LOG.debug("Tagged device {}: {}", device.getId(), tag);

					} else if (attrTypeName.equals("SupportedLanguages")) {
						// SupportedLanguages is an array of values, process it
						// as such.
						String[] supportedLanguages = value.getObject(String[].class);
						device.setStandardAttributeValue(attrTypeId, Joiner.on(',').join(supportedLanguages));
					} else {
						// Otherwise we have a regular string value.
						device.setStandardAttributeValue(attrTypeId, value.getObject(String.class));
					}
				} catch (BusException e) {
					LOG.error("Error occurred while appending about data!", e);
					continue;
				}

			}

			// Now persist all attribute value changes.
			device = deviceClient.update(device).checkedGet();

		} else {
			LOG.warn("No AboutData found!  This is most unexpected.");
		}

		device = deviceClient.get(device.getId(), fetchWithAttrTypes).checkedGet();

		LOG.debug("Created device {}", device);

		return device;
	}

	private void addNewEventSources(Device device) {

		if (device.getObservableEventIds().isEmpty()) {
			LOG.debug("Device {} does not have any events.  Nothing to do.");
			return;
		}

		EventSource eventSource = eventSourceClient.get(eventSourceId).checkedGet();

		String deviceId = device.getId();

		for (String eventId : device.getObservableEventIds()) {
			eventSource.addSourceDevice(deviceId, eventId);
		}

		eventSourceClient.update(eventSource).checkedGet();

		LOG.debug("Updated event source {} with new {} event templates.", eventSourceId,
				device.getObservableEventIds().size());
	}

	private void addDeviceToStream(Device device) {

		StreamDevice streamDevice = null;

		try {
			streamDevice = streamDeviceClient.get(streamId, device.getId()).checkedGet();
		} catch (ServiceException e) {
			// Stream device does not exist.
		}

		if (streamDevice != null) {
			LOG.debug("Device {} already added to stream {}", device.getId(), streamId);
			return;
		}

		streamDevice = new StreamDevice();
		streamDevice.setRealm(realm);
		streamDevice.setCreator(agentName);
		streamDevice.setCreatorApplicationId(agentName);
		streamDevice.setCreationInstant(System.currentTimeMillis());
		streamDevice.setStreamId(streamId);
		streamDevice.setDeviceId(device.getId());

		streamDeviceClient.add(streamId, streamDevice).checkedGet();

		LOG.debug("Added device {} to stream {}", device.getId(), streamId);
	}

	public Device searchDevices(String deviceTemplateId, Map<String, Variant> aboutData) {

		if (aboutData == null) {
			LOG.warn("About data was null, no AppId to search on!  Returning null.");
			return null;
		}

		Variant value = aboutData.get("AppId");

		if (value == null) {
			LOG.warn("No AppId to search on!  Returning null.");
			return null;
		}

		String appId = getAppId(value);

		LOG.debug("Searching for devices that implement template {} and are tagged with {}", deviceTemplateId, appId);

		DeviceFilterSpec filter = new DeviceFilterSpec();
		filter.setActive(true);
		filter.setParentDeviceTemplateIds(deviceTemplateId);
		filter.addTag("AppId:" + appId);

		List<Device> matches = deviceClient.search(filter, Page.ALL).checkedGet();

		if (matches.isEmpty()) {
			LOG.debug("No matches found.");
			return null;
		} else if (matches.size() > 1) {
			LOG.warn("Dang, saw multiple matches for the same tagged AppId, this should not be.");
		}

		// TODO handle this better. Throw exception?
		return matches.get(0);
	}

	public void deactivateProvisionedComponents() {

		/*
		 * Get all active attribute types, event templates, command templates,
		 * device templates and devices, and deactivate them all. This option is
		 * only used during early development stages.
		 */
		AttributeTypeFilterSpec attrFilter = new AttributeTypeFilterSpec();
		attrFilter.setActive(true);

		List<AttributeType> activeAttrs = attributeTypeClient.search(attrFilter, Page.ALL).checkedGet();

		for (AttributeType attr : activeAttrs) {
			attributeTypeClient.deactivateAttributeType(attr.getId());
		}

		CommandTemplateFilterSpec commandFilter = new CommandTemplateFilterSpec();
		commandFilter.setActive(true);

		List<CommandTemplate> activeCommands = commandTemplateClient.search(commandFilter, Page.ALL).checkedGet();

		for (CommandTemplate command : activeCommands) {
			commandTemplateClient.deactivateCommandTemplate(command.getId());
		}

		EventTemplateFilterSpec eventFilter = new EventTemplateFilterSpec();
		eventFilter.setActive(true);

		List<EventTemplate> activeEvents = eventTemplateClient.search(eventFilter, Page.ALL).checkedGet();

		for (EventTemplate event : activeEvents) {
			eventTemplateClient.deactivateEventTemplate(event.getId());
		}

		DeviceTemplateFilterSpec deviceTemplateFilter = new DeviceTemplateFilterSpec();
		deviceTemplateFilter.setActive(true);

		List<DeviceTemplate> activeTemplates = deviceTemplateClient.search(deviceTemplateFilter, Page.ALL).checkedGet();

		for (DeviceTemplate template : activeTemplates) {
			deviceTemplateClient.deactivateDeviceTemplate(template.getId());
		}

		DeviceFilterSpec deviceFilter = new DeviceFilterSpec();
		deviceFilter.setActive(true);

		List<Device> activeDevices = deviceClient.search(deviceFilter, Page.ALL).checkedGet();

		for (Device device : activeDevices) {
			deviceClient.deactivateDevice(device.getId());
		}

		List<StreamDevice> streamDevices = streamDeviceClient.listStreamDevices(streamId).checkedGet();

		for (StreamDevice streamDevice : streamDevices) {
			streamDeviceClient.delete(streamId, streamDevice.getDeviceId());
		}

	}

	private static String getAppId(Variant value) {
		StringBuilder sb = new StringBuilder();

		try {
			byte[] appId = value.getObject(byte[].class);
			for (byte b : appId) {
				sb.append(String.format("%02X", b));
			}
		} catch (BusException e) {
			LOG.error("Error occurred while appending about data!", e);
			return null;
		}

		return sb.toString();
	}

	private DeviceTemplate fetchFullDeviceTemplate(String id) {
		FetchOpts fetchOpts = new FetchOpts().embedAttributeTypes().embedCommandTemplates().embedEventTemplates();
		return deviceTemplateClient.get(id, fetchOpts).checkedGet();
	}

	private List<AttributeType> createAttributeTypes(List<AJProperty> properties, Map<String, Variant> aboutData) {

		List<AttributeType> attrs = new ArrayList<>();

		if (properties != null) {

			for (AJProperty prop : properties) {

				AttributeTypeFilterSpec attrFilter = new AttributeTypeFilterSpec();
				attrFilter.setActive(true);
				attrFilter.setNames(prop.getName());

				List<AttributeType> existing = attributeTypeClient.search(attrFilter, Page.ALL).checkedGet();

				if (!existing.isEmpty()) {
					LOG.debug("An active attribute type already exists for name {}.", prop.getName());
					attrs.add(existing.get(0)); // should be safe
					continue;
				}

				String name = prop.getName();
				DataType dataType = AllJoynSupport.getDataType(prop.getType());

				AttributeType attrType = new AttributeType();
				attrType.setRealm(realm);
				attrType.setCreator(agentName);
				attrType.setCreatorApplicationId(agentName);
				attrType.setCreationInstant(System.currentTimeMillis());
				attrType.setName(name);
				attrType.setDataType(dataType);

				AttributeType created = attributeTypeClient.add(attrType).checkedGet();
				attributeTypeClient.activateAttributeType(created.getId()).checkedGet();
				attributeTypeClient.tagAttributeType(created.getId(), "Created by " + agentName).checkedGet();

				LOG.debug("Created and activated attribute type with id {}", created.getId());

				attrs.add(created);
			}
		}

		if (aboutData != null) {

			for (Map.Entry<String, Variant> entry : aboutData.entrySet()) {

				String key = entry.getKey();

				AttributeTypeFilterSpec attrFilter = new AttributeTypeFilterSpec();
				attrFilter.setActive(true);
				attrFilter.setNames(key);

				List<AttributeType> existing = attributeTypeClient.search(attrFilter, Page.ALL).checkedGet();

				if (!existing.isEmpty()) {
					LOG.debug("An active attribute type already exists for name {}.", key);
					attrs.add(existing.get(0)); // should be safe
					continue;
				}

				String name = key;

				AttributeType attrType = new AttributeType();
				attrType.setRealm(realm);
				attrType.setCreator(agentName);
				attrType.setCreatorApplicationId(agentName);
				attrType.setCreationInstant(System.currentTimeMillis());
				attrType.setName(name);
				attrType.setDataType(DataType.STRING);

				AttributeType created = attributeTypeClient.add(attrType).checkedGet();
				attributeTypeClient.activateAttributeType(created.getId()).checkedGet();
				attributeTypeClient.tagAttributeType(created.getId(), "Created by " + agentName).checkedGet();

				LOG.debug("Created and activated attribute type with id {}", created.getId());

				attrs.add(created);
			}

		} else {
			LOG.warn("No AboutData present!  Something's amiss.  Lock your doors.");
		}

		return attrs;

	}

	private List<CommandTemplate> createCommandTemplates(List<AJMethod> methods) {

		List<CommandTemplate> commands = new ArrayList<>();

		if (methods == null) {
			return commands;
		}

		for (AJMethod method : methods) {

			CommandTemplateFilterSpec commFilter = new CommandTemplateFilterSpec();
			commFilter.setActive(true);
			commFilter.setNames(method.getName());

			List<CommandTemplate> existing = commandTemplateClient.search(commFilter, Page.ALL).checkedGet();

			if (!existing.isEmpty()) {
				LOG.debug("An active command template already exists for name {}.", method.getName());
				commands.add(existing.get(0)); // should be safe
				continue;
			}

			String name = method.getName();

			CommandTemplate command = new CommandTemplate();
			command.setRealm(realm);
			command.setCreator(agentName);
			command.setCreatorApplicationId(agentName);
			command.setCreationInstant(System.currentTimeMillis());
			command.setName(name);

			if (method.getArgs() != null) {

				List<CommandArg> cargs = new ArrayList<>();
				short argIdx = 0;

				for (AJArg arg : method.getArgs()) {

					if ("out".equals(arg.getDirection())) {
						LOG.debug("Ignoring outbound argument {} in method {}", arg.getName(), method.getName());
						continue;
					}

					DataType dataType = AllJoynSupport.getDataType(arg.getType());

					CommandArg carg = new CommandArg();

					String defaultArgName = "arg" + argIdx;

					if (method.getAnnotations() != null) {
						for (AJAnnotation annotation : method.getAnnotations()) {
							if (defaultArgName.equalsIgnoreCase(annotation.getName())) {
								defaultArgName = annotation.getValue();
							}
						}
					}

					carg.setName(defaultArgName);
					carg.setDataType(dataType);
					carg.setIndex(argIdx);
					carg.setActive(true);
					carg.setRealm(realm);
					carg.setCreationInstant(System.currentTimeMillis());
					carg.setCreator(agentName);
					carg.setCreatorApplicationId(agentName);

					cargs.add(carg);
					argIdx++;
				}

				command.setArgs(cargs);

			}

			CommandTemplate created = commandTemplateClient.add(command).checkedGet();
			commandTemplateClient.activateCommandTemplate(created.getId()).checkedGet();
			commandTemplateClient.tagCommandTemplate(created.getId(), "Created by " + agentName).checkedGet();

			LOG.debug("Created and activated command template with id {}", created.getId());

			commands.add(created);
		}

		return commands;

	}

	private List<EventTemplate> createEventTemplates(List<AJSignal> signals) {

		List<EventTemplate> events = new ArrayList<>();

		if (signals == null) {
			return events;
		}

		for (AJSignal signal : signals) {

			String name = signal.getName();

			EventTemplateFilterSpec eventFilter = new EventTemplateFilterSpec();
			eventFilter.setActive(true);
			eventFilter.setNames(name);

			List<EventTemplate> existing = eventTemplateClient.search(eventFilter, Page.ALL).checkedGet();

			if (!existing.isEmpty()) {
				LOG.debug("An active event template already exists for name {}.", name);
				events.add(existing.get(0)); // should be safe
				continue;
			}

			EventTemplate event = new EventTemplate();
			event.setRealm(realm);
			event.setCreator(agentName);
			event.setCreatorApplicationId(agentName);
			event.setCreationInstant(System.currentTimeMillis());
			event.setName(name);

			int idx = 0;

			if (signal.getArgs() != null) {

				List<EventField> fields = new ArrayList<>();

				for (AJArg arg : signal.getArgs()) {

					if ("in".equalsIgnoreCase(arg.getDirection())) {
						LOG.debug("Ignoring inbound argument {} in signal {}", arg.getName(), signal.getName());
						continue;
					}

					DataType dataType = AllJoynSupport.getDataType(arg.getType());

					EventField field = new EventField();

					String defaultFieldName = "arg" + idx++;

					if (signal.getAnnotations() != null) {
						for (AJAnnotation annotation : signal.getAnnotations()) {
							if (defaultFieldName.equalsIgnoreCase(annotation.getName())) {
								defaultFieldName = annotation.getValue();
							}
						}
					}

					field.setName(defaultFieldName);
					field.setDataType(dataType);
					field.setActive(true);
					field.setRealm(realm);
					field.setCreationInstant(System.currentTimeMillis());
					field.setCreator(agentName);
					field.setCreatorApplicationId(agentName);

					fields.add(field);
				}

				event.setEventFields(fields);

			}

			EventTemplate created = eventTemplateClient.add(event).checkedGet();
			eventTemplateClient.activateEventTemplate(created.getId()).checkedGet();
			eventTemplateClient.tagEventTemplate(created.getId(), "Created by " + agentName).checkedGet();

			LOG.debug("Created and activated event template with id {}", created.getId());

			events.add(created);
		}

		return events;
	}

	private class DeviceTemplateMatcher implements Predicate<DeviceTemplate> {

		private final AJInterface intf;

		public DeviceTemplateMatcher(AJInterface intf) {
			this.intf = intf;
		}

		public boolean apply(DeviceTemplate input) {

			if (input == null) {
				return false;
			}

			DeviceTemplate fullTemplate = fetchFullDeviceTemplate(input.getId());

			double propertiesRating = attributeTypeMatchRating(intf.getProperties(), fullTemplate.getAttributeTypes());

			double signalsRating = eventTemplateMatchRating(intf.getSignals(), fullTemplate.getEventTemplates());

			double methodsRating = commandTemplateMatchRating(intf.getMethods(), fullTemplate.getCommandTemplates());

			boolean match = true;

			match &= propertiesRating > propertyMatchRatingThreshold;
			match &= signalsRating > signalMatchRatingThreshold;
			match &= methodsRating > methodMatchRatingThreshold;

			return match;
		}

		private double attributeTypeMatchRating(List<AJProperty> properties, List<AttributeType> attrTypes) {
			// TODO implement
			return propertyMatchRatingThreshold;
		}

		private double eventTemplateMatchRating(List<AJSignal> signals, List<EventTemplate> eventTemplates) {

			if (signals == null) {
				return 100;
			}

			int matchCount = 0;

			for (AJSignal signal : signals)
				outerLoop: {

					final String signalName = signal.getName();

					if (signalName == null) {
						continue;
					}

					EventTemplate eventTemplate = AllJoynSupport.getEventTemplateForSignal(signal, eventTemplates);

					if (eventTemplate == null) {
						LOG.warn("Uh Ooooh, did not find any event template with name {}", signalName);
						continue;
					}

					boolean result = AllJoynSupport.matchEventFields(eventTemplate, signal, null);

					if (result == true) {
						matchCount++;
					} else {
						break outerLoop;
					}

					matchCount++;

				}

			return matchCount / signals.size() * 100;
		}

		private double commandTemplateMatchRating(List<AJMethod> methods, List<CommandTemplate> commandTemplates) {

			if (methods == null) {
				return 100;
			}

			int matchCount = 0;

			for (AJMethod method : methods)
				outerLoop: {

					final String methodName = method.getName();

					if (methodName == null) {
						continue;
					}

					CommandTemplate commandTemplate = AllJoynSupport.getCommandTemplateForMethod(method,
							commandTemplates);

					if (commandTemplate == null) {
						LOG.warn("Uh Ooooh, did not find any command template with name {}", methodName);
						continue;
					}

					boolean result = AllJoynSupport.matchCommandArgs(commandTemplate, method, null);

					if (result == true) {
						matchCount++;
					} else {
						break outerLoop;
					}

				}

			return matchCount / methods.size() * 100;
		}

	}

	private static class IdGetter implements Function<Resource, String> {

		private static final IdGetter INSTANCE = new IdGetter();

		public String apply(Resource input) {
			if (input == null) {
				return null;
			}
			return input.getId();
		}

	}

	public static class IsInputArg implements Predicate<AJArg> {

		public static final IsInputArg INSTANCE = new IsInputArg();

		public boolean apply(AJArg input) {
			if (input == null) {
				return false;
			}
			return "in".equals(input.getDirection());
		}
	}

	public static class IsOutputArg implements Predicate<AJArg> {

		public static final IsOutputArg INSTANCE = new IsOutputArg();

		public boolean apply(AJArg input) {
			if (input == null) {
				return false;
			}
			return "out".equals(input.getDirection());
		}
	}

}
