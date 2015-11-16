package com.covisint.platform.gateway.bind;

import javax.json.JsonObject;

public interface CommandDelegate {

	void process(JsonObject command);

}
