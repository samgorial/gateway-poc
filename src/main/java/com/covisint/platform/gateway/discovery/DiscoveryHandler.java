package com.covisint.platform.gateway.discovery;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DiscoveryHandler {

	private final ExecutorService executor;

	DiscoveryHandler() {
		executor = Executors.newFixedThreadPool(2);
	}

	void handleAsync(final IntrospectResult metadata) {

		executor.submit(new Callable<Void>() {

			public Void call() throws Exception {
				return null;
			}

		});

	}

}
