package com.covisint.platform.gateway.bind;

import org.alljoyn.bus.BusObject;

import com.covisint.platform.gateway.domain.alljoyn.AJMethod;

public interface Command<T extends BusObject> {

	void apply(T target);

	AJMethod getSourceMethod();

	Object[] getArgs();

}
