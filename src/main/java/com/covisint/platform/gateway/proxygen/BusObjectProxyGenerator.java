package com.covisint.platform.gateway.proxygen;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.annotation.BusInterface;
import org.springframework.stereotype.Component;

import com.covisint.platform.gateway.proxygen.ProxySpec.MethodSpec;

@Component
public class BusObjectProxyGenerator implements ProxyClassGenerator {

	private static final String BUS_EXCEPTION = org.alljoyn.bus.BusException.class.getName();

	private static final String BUS_OBJECT = org.alljoyn.bus.BusObject.class.getName();

	private static final String BUS_INTERFACE = org.alljoyn.bus.annotation.BusInterface.class.getName();

	private static final String BUS_METHOD = org.alljoyn.bus.annotation.BusMethod.class.getName();

	private static final String BUS_PROPERTY = org.alljoyn.bus.annotation.BusProperty.class.getName();

	private static final String BUS_SIGNAL = org.alljoyn.bus.annotation.BusSignal.class.getName();

	public Class<? extends BusObject> generateAndExport(ProxySpec spec, URL output) {

		final StringBuilder source = new StringBuilder();

		source.append("package ").append(spec.fqdn).append(";").append("\n\n");

		source.append("import ").append(BUS_EXCEPTION).append(";").append("\n");
		source.append("import ").append(BUS_OBJECT).append(";").append("\n");
		source.append("import ").append(BUS_INTERFACE).append(";").append("\n");
		source.append("import ").append(BUS_METHOD).append(";").append("\n");
		source.append("import ").append(BUS_PROPERTY).append(";").append("\n");
		source.append("import ").append(BUS_SIGNAL).append(";").append("\n\n");

		source.append("@").append(BusInterface.class.getSimpleName()).append("(name = \"").append(spec.fqdn)
				.append("\")").append("\n");

		source.append("public interface AJProxy extends ").append(BusObject.class.getSimpleName()).append(" {\n\n");
		
		for (final MethodSpec method : spec.methods) {
			addMethod(source, method);
		}
		
		return null;
		
	}

	public static void main(String[] args) {
		
		try {
			File f = new File(BusObject.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			System.out.println(f.getAbsolutePath());
			
			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

			compiler.run(null, null, null, "/home/sam/Test.java");
			
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		System.out.println();
	}

	private void addMethod(StringBuilder source, MethodSpec spec) {

		switch (spec.options.get("method_type")) {
		case "property":

			break;
		case "method":
			break;
		case "signal":
			break;
		default:
			throw new IllegalArgumentException("Unsupported method type " + spec.options.get("method_type"));
		}

	}

}
