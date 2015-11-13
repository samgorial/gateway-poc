package com.covisint.platform.gateway.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.covisint.core.http.service.core.InternationalString;
import com.covisint.platform.device.core.DataType;
import com.covisint.platform.device.core.attributetype.AttributeType;
import com.covisint.platform.device.core.commandtemplate.CommandArg;
import com.covisint.platform.device.core.commandtemplate.CommandTemplate;
import com.covisint.platform.device.core.device.Device;
import com.covisint.platform.device.core.device.DeviceAttribute;
import com.covisint.platform.gateway.domain.alljoyn.AJArg;
import com.covisint.platform.gateway.domain.alljoyn.AJInterface;
import com.covisint.platform.gateway.domain.alljoyn.AJMethod;
import com.covisint.platform.gateway.domain.alljoyn.AJProperty;

@Component
public class DeviceProvisionerMockDao implements DeviceProvisionerDao {

	private Set<String> blackListInterfaces;

	public DeviceProvisionerMockDao() {
		blackListInterfaces = new HashSet<String>(
				Arrays.asList("org.freedesktop.DBus.Introspectable", "org.allseen.Introspectable"));
	}

	public Device provisionNewDevice(AJInterface intf) throws InterfaceBlacklistedException {

		if (blackListInterfaces.contains(intf.getName())) {
			throw new InterfaceBlacklistedException(intf.getName());
		}

		Device device = new Device();

		device.setId(UUID.randomUUID().toString());
		device.setCreationInstant(System.currentTimeMillis() - 500);
		device.setRealm("ABC-REALM");
		device.setCreator("ADMIN");
		device.setCreatorApplicationId("GATEWAY-AGENT");
		device.setVersion("");
		device.setParentDeviceTemplateId(UUID.randomUUID().toString());

		device.setName(new InternationalString(Locale.US.toString(), intf.getName()));
		device.setDescription(new InternationalString(Locale.US.toString(), intf.getName()));

		if (intf.getProperties() != null) {

			for (AJProperty prop : intf.getProperties()) {

				AttributeType attrType = new AttributeType();

				attrType.setName(prop.getName());

				getDataType(prop.getType());

				device.addStandardAttribute(new DeviceAttribute(attrType));

			}

		}

		for (AJMethod method : intf.getMethods()) {

			CommandTemplate command = new CommandTemplate();
			command.setName(method.getName());

			List<CommandArg> commandArgs = new ArrayList<>();

			if (method.getArgs() != null) {
				for (AJArg arg : method.getArgs()) {
					CommandArg commandArg = new CommandArg();
					commandArg.setName(arg.getName());
					commandArg.setDataType(getDataType(arg.getType()));
					commandArgs.add(commandArg);
				}
			}

			command.setArgs(commandArgs);

			device.addSupportedCommand(command);

		}

		return device;
	}

	private DataType getDataType(String ajType) {

		switch (ajType) {
		case "s":
		case "y":
			return DataType.STRING;
		case "b":
			return DataType.BOOL;
		case "n":
		case "q":
		case "i":
		case "u":
		case "x":
		case "t":
			return DataType.INTEGER;
		case "d":
			return DataType.DECIMAL;
		}

		return null;
	}

}
