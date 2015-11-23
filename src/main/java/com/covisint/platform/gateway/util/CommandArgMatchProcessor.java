package com.covisint.platform.gateway.util;

import com.covisint.platform.device.core.DataType;

public interface CommandArgMatchProcessor {

	void onMatch(String methodArgName, String commandArgName, String methodArgType, DataType commandArgType);

}
