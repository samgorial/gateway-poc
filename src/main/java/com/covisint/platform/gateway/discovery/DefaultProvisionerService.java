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
import com.covisint.platform.gateway.domain.alljoyn.AJAnnotation;
import com.covisint.platform.gateway.domain.alljoyn.AJArg;
import com.covisint.platform.gateway.domain.alljoyn.AJInterface;
import com.covisint.platform.gateway.domain.alljoyn.AJMethod;
import com.covisint.platform.gateway.domain.alljoyn.AJProperty;
import com.covisint.platform.gateway.domain.alljoyn.AJSignal;
import com.covisint.platform.gateway.util.AllJoynSupport;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

@Component
public class DefaultProvisionerService implements ProvisionerService {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultProvisionerService.class);

	@Value("${agent.name}")
	private String agentName;

	@Value("${agent.realm}")
	private String realm;

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

	public DeviceTemplate searchDeviceTemplates(AJInterface intf) {

		LOG.debug("About to search for device templates matching interface {}", intf.getName());

		DeviceTemplateFilterSpec filter = new DeviceTemplateFilterSpec();

		// Only want active templates.
		filter.setActive(true);

		List<DeviceTemplate> allTemplates = deviceTemplateClient.search(filter, Page.ALL).checkedGet();

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

		return filtered.get(0);
	}

	public DeviceTemplate createDeviceTemplate(AJInterface intf) {

		LOG.info("Creating new device template for interface {} with details: \n{}", intf.getName(), intf);

		List<AttributeType> attrTypes = createAttributeTypes(intf.getProperties(), intf.getAboutData());
		List<CommandTemplate> commandTemplates = createCommandTemplates(intf.getMethods());
		List<EventTemplate> eventTemplates = createEventTemplates(intf.getSignals());

		DeviceTemplateFilterSpec filter = new DeviceTemplateFilterSpec();
		filter.setActive(true);
		filter.setAttributeTypeIds(FluentIterable.from(attrTypes).transform(IdGetter.INSTANCE).toSet());
		filter.setCommandTemplateIds(FluentIterable.from(commandTemplates).transform(IdGetter.INSTANCE).toSet());
		filter.setEventTemplateIds(FluentIterable.from(eventTemplates).transform(IdGetter.INSTANCE).toSet());

		List<DeviceTemplate> existing = deviceTemplateClient.search(filter, Page.ALL).checkedGet();

		if (!existing.isEmpty()) {
			LOG.warn("Already found device template containing attribute types, "
					+ "events and commands specified in interface {}", intf.getName());
			return fetchFullDeviceTemplate(existing.get(0).getId());
		}

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

		deviceTemplateClient.activateDeviceTemplate(created.getId()).checkedGet();
		deviceTemplateClient.tagDeviceTemplate(created.getId(), "Created by " + agentName).checkedGet();

		LOG.debug("Created and activated device template successfully with id {}", created.getId());

		DeviceTemplate retrieved = fetchFullDeviceTemplate(created.getId());

		LOG.debug("Returning full device template resource: \n{}", retrieved);

		return retrieved;
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

	private static class IdGetter implements Function<Resource, String> {

		private static final IdGetter INSTANCE = new IdGetter();

		public String apply(Resource input) {
			if (input == null) {
				return null;
			}
			return input.getId();
		}

	}

	public Device createDevice(String deviceTemplateId, Map<String, Variant> aboutData) {

		LOG.debug("Creating new device.");

		Device device = deviceClient.createDeviceFromTemplate(deviceTemplateId).checkedGet();

		deviceClient.activateDevice(device.getId());

		LOG.debug("Created and activated device with id {}", device.getId());

		device = deviceClient.get(device.getId(), new FetchOpts().embedAttributeTypes()).checkedGet();

		if (aboutData != null) {

			for (AttributeType attrType : device.getStandardAttributeTypes()) {

				String attrTypeName = attrType.getName();

				Variant value = aboutData.get(attrTypeName);

				if (value == null) {
					LOG.debug("Hmm, no about data variant found for attribute type {} of device {}", attrTypeName,
							device.getId());
					continue;
				}

				try {
					if (attrTypeName.equals("AppId")) {
						byte[] appId = value.getObject(byte[].class);
						StringBuilder sb = new StringBuilder();
						for (byte b : appId) {
							sb.append(String.format("%02X", b));
						}
						device.setStandardAttributeValue(attrTypeName, sb.toString());

						// Also need to tag with the AppId (it's special)
						String tag = attrTypeName + ":" + sb.toString();
						deviceClient.tagDevice(device.getId(), tag).checkedGet();
						LOG.debug("Tagged device {}: {}", device.getId(), tag);

					} else if (attrTypeName.equals("SupportedLanguages")) {
						String[] supportedLanguages = value.getObject(String[].class);
						device.setStandardAttributeValue(attrTypeName, Joiner.on(',').join(supportedLanguages));
					} else {
						device.setStandardAttributeValue(attrTypeName, value.getObject(String.class));
					}
				} catch (BusException e) {
					LOG.error("Error occurred while appending about data!", e);
					continue;
				}

			}

			device = deviceClient.update(device).checkedGet();

		}

		deviceClient.tagDevice(device.getId(), "Created by " + agentName).checkedGet();

		device = deviceClient.get(device.getId(), new FetchOpts().embedAttributeTypes()).checkedGet();

		LOG.debug("Created device {}", device);

		return device;
	}

	public Device searchDevices(String deviceTemplateId, Map<String, Variant> aboutData) {

		if (aboutData == null) {
			LOG.warn("About data was null, no AppId to search on!");
			return null;
		}

		Variant value = aboutData.get("AppId");

		if (value == null) {
			LOG.warn("No AppId to search on!");
			return null;
		}

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

		LOG.debug("Searching for devices that implement template {} and are tagged with {}", deviceTemplateId,
				sb.toString());

		DeviceFilterSpec filter = new DeviceFilterSpec();
		filter.setActive(true);
		filter.setParentDeviceTemplateIds(deviceTemplateId);
		filter.addTag(sb.toString());

		List<Device> matches = deviceClient.search(filter, Page.ALL).checkedGet();

		if (matches.isEmpty()) {
			LOG.debug("No matches found.");
			return null;
		} else if (matches.size() > 1) {
			LOG.warn("Dang, saw multiple matches for the same tagged AppId, this should not be.");
		}

		return matches.get(0);
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

			double propertiesRating = attributeTypeMatchRating(intf.getProperties(), input.getAttributeTypes());

			double signalsRating = eventTemplateMatchRating(intf.getSignals(), input.getEventTemplates());

			double methodsRating = commandTemplateMatchRating(intf.getMethods(), input.getCommandTemplates());

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

					Optional<EventTemplate> optional = FluentIterable.from(eventTemplates)
							.firstMatch(new Predicate<EventTemplate>() {

								public boolean apply(EventTemplate input) {
									return input.getName().equals(signalName);
								}

							});

					if (!optional.isPresent()) {
						LOG.warn("Uh Ooooh, did not find any event template with name {}", signalName);
						continue;
					}

					EventTemplate eventTemplate = optional.get();

					if (signal.getArgs() != null) {

						// Count number of signal arguments.
						List<AJArg> signalArgs = FluentIterable.from(signal.getArgs()).filter(new Predicate<AJArg>() {

							public boolean apply(AJArg input) {
								if (input == null) {
									return false;
								}
								return "out".equals(input.getDirection());
							}

						}).toList();

						// Compare to number of event template arguments.
						if (eventTemplate.getEventFields().size() != signalArgs.size()) {
							LOG.warn(
									"OOOPS!  Event template had {} fields but signal had {} (outbound).  Skipping event template.",
									eventTemplate.getEventFields().size(), signalArgs.size());
							continue;
						}

						int idx = 0;
						for (AJArg arg : signalArgs) {

							DataType signalArgType = AllJoynSupport.getDataType(arg.getType());
							DataType eventFieldType = eventTemplate.getEventFields().get(idx).getDataType();

							if (signalArgType != eventFieldType) {
								LOG.warn("Shoot, signal and event field types differ at index {}: {} vs {}", idx,
										signalArgType, eventFieldType);
								break outerLoop;
							}

							String signalArgName = "arg" + idx;
							String eventFieldName = eventTemplate.getEventFields().get(idx).getName();

							if (signal.getAnnotations() != null) {
								for (AJAnnotation annotation : signal.getAnnotations()) {
									if (signalArgName.equalsIgnoreCase(annotation.getName())) {
										signalArgName = annotation.getValue();
									}
								}
							}

							if (!signalArgName.equalsIgnoreCase(eventFieldName)) {
								LOG.warn("Oh so close!  Signal and event field names differ at index {}: {} vs {}", idx,
										signalArgName, eventFieldName);
								break outerLoop;
							}

							idx++;
						}

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

					Optional<CommandTemplate> optional = FluentIterable.from(commandTemplates)
							.firstMatch(new Predicate<CommandTemplate>() {

								public boolean apply(CommandTemplate input) {
									return input.getName().equals(methodName);
								}

							});

					if (!optional.isPresent()) {
						LOG.warn("Uh Ooooh, did not find any command template with name {}", methodName);
						continue;
					}

					CommandTemplate commandTemplate = optional.get();

					if (method.getArgs() != null) {

						// Count number of method arguments.
						List<AJArg> methodArgs = FluentIterable.from(method.getArgs()).filter(new Predicate<AJArg>() {

							public boolean apply(AJArg input) {
								if (input == null) {
									return false;
								}
								return "in".equals(input.getDirection());
							}

						}).toList();

						// Compare to number of command template arguments.
						if (commandTemplate.getArgs().size() != methodArgs.size()) {
							LOG.warn(
									"OOOPS!  Command template had {} args but AJ method had {} (inbound).  Skipping command template.",
									commandTemplate.getArgs().size(), methodArgs.size());
							continue;
						}

						int idx = 0;
						for (AJArg arg : methodArgs) {

							DataType methodArgType = AllJoynSupport.getDataType(arg.getType());
							DataType commandArgType = commandTemplate.getArgs().get(idx).getDataType();

							if (methodArgType != commandArgType) {
								LOG.warn("Shoot, method and command arg types differ at index {}: {} vs {}", idx,
										methodArgType, commandArgType);
								break outerLoop;
							}

							String methodArgName = "arg" + idx;
							String commandArgName = commandTemplate.getArgs().get(idx).getName();

							if (method.getAnnotations() != null) {
								for (AJAnnotation annotation : method.getAnnotations()) {
									if (methodArgName.equalsIgnoreCase(annotation.getName())) {
										methodArgName = annotation.getValue();
									}
								}
							}

							if (!methodArgName.equalsIgnoreCase(commandArgName)) {
								LOG.warn("Oh so close!  Method and command arg names differ at index {}: {} vs {}", idx,
										methodArgName, commandArgName);
								break outerLoop;
							}

							idx++;
						}

					}

					matchCount++;
				}

			return matchCount / methods.size() * 100;
		}

	}

}
