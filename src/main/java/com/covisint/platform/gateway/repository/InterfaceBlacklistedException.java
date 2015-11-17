package com.covisint.platform.gateway.repository;

public class InterfaceBlacklistedException extends Exception {

	private static final long serialVersionUID = 1L;

	public InterfaceBlacklistedException() {
		super();
	}

	public InterfaceBlacklistedException(String message) {
		super(message);
	}

	public InterfaceBlacklistedException(Exception cause) {
		super(cause);
	}

	public InterfaceBlacklistedException(String message, Exception cause) {
		super(message, cause);
	}

}
