package com.covisint.platform.gateway.discovery;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.covisint.core.http.service.core.InternationalString;
import com.covisint.core.http.service.core.Page;
import com.covisint.platform.device.client.FetchOpts;
import com.covisint.platform.device.client.attributetype.AttributeTypeSDK.AttributeTypeClient;
import com.covisint.platform.device.client.attributetype.AttributeTypeSDK.AttributeTypeClient.AttributeTypeFilterSpec;
import com.covisint.platform.device.client.commandtemplate.CommandTemplateSDK.CommandTemplateClient;
import com.covisint.platform.device.client.commandtemplate.CommandTemplateSDK.CommandTemplateClient.CommandTemplateFilterSpec;
import com.covisint.platform.device.client.devicetemplate.DeviceTemplateSDK.DeviceTemplateClient;
import com.covisint.platform.device.client.devicetemplate.DeviceTemplateSDK.DeviceTemplateClient.DeviceTemplateFilterSpec;
import com.covisint.platform.device.client.eventtemplate.EventTemplateSDK.EventTemplateClient;
import com.covisint.platform.device.client.eventtemplate.EventTemplateSDK.EventTemplateClient.EventTemplateFilterSpec;
import com.covisint.platform.device.core.DataType;
import com.covisint.platform.device.core.attributetype.AttributeType;
import com.covisint.platform.device.core.commandtemplate.CommandArg;
import com.covisint.platform.device.core.commandtemplate.CommandTemplate;
import com.covisint.platform.device.core.devicetemplate.DeviceTemplate;
import com.covisint.platform.device.core.eventtemplate.EventField;
import com.covisint.platform.device.core.eventtemplate.EventTemplate;
import com.covisint.platform.gateway.domain.alljoyn.AJArg;
import com.covisint.platform.gateway.domain.alljoyn.AJInterface;
import com.covisint.platform.gateway.domain.alljoyn.AJMethod;
import com.covisint.platform.gateway.domain.alljoyn.AJProperty;
import com.covisint.platform.gateway.domain.alljoyn.AJSignal;
import com.covisint.platform.gateway.util.AllJoynSupport;

@Component
public class DefaultProvisionerService implements ProvisionerService {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultProvisionerService.class);

	@Value("${agent.name}")
	private String agentName;

	@Value("${agent.realm}")
	private String realm;

	@Autowired
	private AttributeTypeClient attributeTypeClient;

	@Autowired
	private CommandTemplateClient commandTemplateClient;

	@Autowired
	private EventTemplateClient eventTemplateClient;

	@Autowired
	private DeviceTemplateClient deviceTemplateClient;

	public DeviceTemplate searchDeviceTemplates(AJInterface intf) {

		LOG.debug("About to search for device templates matching interface {}", intf.getName());

		DeviceTemplateFilterSpec filter = new DeviceTemplateFilterSpec();

		// Only want active templates.
		filter.setActive(true);

		addAttributeTypes(intf.getProperties(), filter);

		addCommandTemplates(intf.getMethods(), filter);

		addEventTemplates(intf.getSignals(), filter);

		boolean validFilter = validateFilter(filter);

		if (!validFilter) {
			LOG.info("Could not build valid filter based on interface metadata.");
			return null;
		}

		List<DeviceTemplate> matchingTemplates = deviceTemplateClient.search(filter, Page.ALL).checkedGet();

		if (matchingTemplates.isEmpty()) {
			LOG.debug("Could not match any templates on interface {}", intf.getName());
			return null;
		} else if (matchingTemplates.size() > 1) {
			LOG.warn(
					"Problem, there are multiple ({}) matched device templates for interface {}, only expected 1.  Using the first one.",
					matchingTemplates.size(), intf.getName());
		}

		return matchingTemplates.get(0);
	}

	private boolean validateFilter(DeviceTemplateFilterSpec filter) {

		boolean valid = false;

		valid |= !filter.getAttributeTypeIds().isEmpty();

		valid |= !filter.getCommandTemplateIds().isEmpty();

		valid |= !filter.getEventTemplateIds().isEmpty();

		return valid;
	}

	private void addAttributeTypes(List<AJProperty> properties, DeviceTemplateFilterSpec filter) {
	
		if(properties == null) {
			return;
		}

		// TODO implement
	
	}

	private void addEventTemplates(List<AJSignal> signals, DeviceTemplateFilterSpec filter) {

		if(signals == null) {
			return;
		}
		
		for (AJSignal signal : signals) {

			String signalName = signal.getName();

			EventTemplateFilterSpec eventTemplateFilter = new EventTemplateFilterSpec();
			eventTemplateFilter.setActive(true);
			eventTemplateFilter.setNames(signalName);

			List<EventTemplate> eventTemplates = eventTemplateClient.search(eventTemplateFilter, Page.ALL).checkedGet();

			if (eventTemplates.isEmpty()) {
				LOG.warn("Uh Ooooh, did not find any event template with name {}", signalName);
				continue;
			} else if (eventTemplates.size() > 1) {
				LOG.warn("Oh NOOOO's, only expected one event template with name {} but got {}. "
						+ "Will only use the first one.", signalName, eventTemplates.size());
			}

			EventTemplate eventTemplate = eventTemplates.get(0);

			int signalArgs = 0;

			if (signal.getArgs() != null) {

				for (AJArg arg : signal.getArgs()) {
					if ("o".equals(arg.getType())) {
						signalArgs++;
					}
				}

			}

			if (eventTemplate.getEventFields().size() != signalArgs) {
				LOG.warn("OOOPS!  Event template had {} fields but signal had {} (outbound).  Skipping event template.",
						eventTemplate.getEventFields().size(), signalArgs);
				continue;
			}

			// TODO check arg types
			// TODO check arg names??

			filter.addEventTemplateId(eventTemplates.get(0).getId());

		}

	}

	private void addCommandTemplates(List<AJMethod> methods, DeviceTemplateFilterSpec filter) {

		if(methods == null) {
			return;
		}

		for (AJMethod method : methods) {

			String methodName = method.getName();

			CommandTemplateFilterSpec commandTemplateFilter = new CommandTemplateFilterSpec();
			commandTemplateFilter.setActive(true);
			commandTemplateFilter.setNames(methodName);

			List<CommandTemplate> commandTemplates = commandTemplateClient.search(commandTemplateFilter, Page.ALL)
					.checkedGet();

			if (commandTemplates.isEmpty()) {
				LOG.warn("Uh Ooooh, did not find any command template with name {}", methodName);
				continue;
			} else if (commandTemplates.size() > 1) {
				LOG.warn("Oh NOOOO's, only expected one command template with name {} but got {}. "
						+ "Will only use the first one.", methodName, commandTemplates.size());
			}

			CommandTemplate commandTemplate = commandTemplates.get(0);

			int methodArgs = 0;

			if (method.getArgs() != null) {
				for (AJArg arg : method.getArgs()) {
					if ("i".equals(arg.getType())) {
						methodArgs++;
					}
				}
			}

			if (commandTemplate.getArgs().size() != methodArgs) {
				LOG.warn(
						"OOOPS!  Command template had {} args but AJ method had {} (inbound).  Skipping command template.",
						commandTemplate.getArgs().size(), methodArgs);
				continue;
			}

			// TODO check arg types
			// TODO check arg names??

			filter.addCommandTemplateId(commandTemplates.get(0).getId());
		}

	}

	public DeviceTemplate createDeviceTemplate(AJInterface intf) {

		LOG.info("Creating new device template for interface {} with details: \n{}", intf.getName(), intf);

		List<AttributeType> attrTypes = createAttributeTypes(intf.getProperties());
		List<CommandTemplate> commandTemplates = createCommandTemplates(intf.getMethods());
		List<EventTemplate> eventTemplates = createEventTemplates(intf.getSignals());

		DeviceTemplate template = new DeviceTemplate();

		template.setRealm(realm);
		template.setName(new InternationalString("en", intf.getName()));
		template.setDescription(new InternationalString("en", "Auto-created by " + agentName));
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

		FetchOpts fetchOpts = new FetchOpts().embedAttributeTypes().embedCommandTemplates().embedEventTemplates();

		DeviceTemplate retrieved = deviceTemplateClient.get(created.getId(), fetchOpts).checkedGet();

		LOG.debug("Returning full device template resource: \n{}", retrieved);

		return retrieved;
	}

	private List<AttributeType> createAttributeTypes(List<AJProperty> properties) {

		List<AttributeType> attrs = new ArrayList<>();

		if(properties == null) {
			return attrs;
		}

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

		return attrs;
	}

	private List<CommandTemplate> createCommandTemplates(List<AJMethod> methods) {

		List<CommandTemplate> commands = new ArrayList<>();

		if(methods == null) {
			return commands;
		}

		for (AJMethod method : methods) {

			CommandTemplateFilterSpec commFilter = new CommandTemplateFilterSpec();
			commFilter.setActive(true);
			commFilter.setNames(method.getName());

			List<CommandTemplate> existing = commandTemplateClient.search(commFilter, Page.ALL).checkedGet();

			if (!existing.isEmpty()) {
				LOG.debug("An active command template already exists for name {}.",
						method.getName());
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

					if ("o".equalsIgnoreCase(arg.getDirection())) {
						LOG.debug("Ignoring outbound argument {} in method {}", arg.getName(), method.getName());
						continue;
					}

					DataType dataType = AllJoynSupport.getDataType(arg.getType());

					CommandArg carg = new CommandArg();

					// TODO fix me
					if (arg.getName() == null || arg.getName().isEmpty()) {
						arg.setName(System.currentTimeMillis() + "");
					}

					carg.setName(arg.getName());
					carg.setDataType(dataType);
					carg.setIndex(argIdx++);
					carg.setActive(true);
					carg.setRealm(realm);
					carg.setCreationInstant(System.currentTimeMillis());
					carg.setCreator(agentName);
					carg.setCreatorApplicationId(agentName);

					cargs.add(carg);
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

		if(signals == null) {
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

			if (signal.getArgs() != null) {

				List<EventField> fields = new ArrayList<>();

				for (AJArg arg : signal.getArgs()) {

					if ("i".equalsIgnoreCase(arg.getDirection())) {
						LOG.debug("Ignoring inbound argument {} in signal {}", arg.getName(), signal.getName());
						continue;
					}

					DataType dataType = AllJoynSupport.getDataType(arg.getType());

					EventField field = new EventField();

					// TODO fix me
					if (arg.getName() == null || arg.getName().isEmpty()) {
						arg.setName(System.currentTimeMillis() + "");
					}

					field.setName(arg.getName());
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
}
