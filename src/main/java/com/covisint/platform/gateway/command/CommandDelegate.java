package com.covisint.platform.gateway.command;

import javax.json.JsonObject;

public interface CommandDelegate {

	void process(JsonObject command);

}
