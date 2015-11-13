package com.covisint.platform.gateway.proxygen;

import java.util.Map;

public class ProxySpec {

	public String fqdn;
	
	public String className;

	public MethodSpec[] methods;
	
	public Map<String, String> options;
	
	public static class MethodSpec {
	
		public String name;

		public String visibility;
		
		public String returnTypeId;
		
		public String[] paramTypeIds;
		
		public String[] exceptionTypes;

		public Map<String, String> options;
		
	}
	
}
