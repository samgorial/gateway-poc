package com.covisint.platform.gateway.bind;

import org.alljoyn.bus.BusObject;
import org.springframework.stereotype.Component;

import com.covisint.platform.gateway.domain.alljoyn.AJMethod;

@Component
public class CommandFactory<T extends BusObject> {

	@SuppressWarnings("unchecked")
	public final Command<T> create(AJMethod method, Object[] args) {
		return (Command<T>) new DefaultCommand(method, args);
	}

	private class DefaultCommand implements Command<PiBusInterface> {

		private final AJMethod method;

		private final Object[] args;

		private DefaultCommand(AJMethod method, Object[] args) {
			this.method = method;
			this.args = args;
		}

		public AJMethod getSourceMethod() {
			return method;
		}

		public Object[] getArgs() {
			return args;
		}

		public void apply(PiBusInterface target) {
			throw new UnsupportedOperationException();
		}

	}

}
