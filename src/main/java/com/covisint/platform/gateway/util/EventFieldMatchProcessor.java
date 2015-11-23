package com.covisint.platform.gateway.util;

import com.covisint.platform.device.core.DataType;

public interface EventFieldMatchProcessor {

	void onMatch(String signalArgName, String eventFieldName, String signalArgType, DataType eventFieldType);

}
