package mock;

import java.util.HashMap;
import java.util.Map;

import org.alljoyn.bus.SessionOpts;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Globals {

	private static final Logger LOG = LoggerFactory.getLogger(Globals.class);
	
	public static final Map<Integer, SessionInfo> SESSION_BY_ID = new HashMap<>();

	public static final class SessionInfo {

		public Integer sessionId;

		public String busName;
		
		public short transport;
		
		public String namePrefix;

		public String path;

		public Short port;

		public SessionOpts sessionOpts;

		public Class<?>[] busInterfaces;
		
		public String toString() {
			return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
		}

	}

	public static final void addSession(SessionInfo session) {
		
		if(SESSION_BY_ID.containsKey(session.sessionId)) {
			throw new RuntimeException("Map already contains session id " + session.sessionId);
		}
		
		SESSION_BY_ID.put(session.sessionId, session);
		
		LOG.info("New session added: \n{}", session);
	}
	
	public static final void removeSession(int sessionId) {
		if(!SESSION_BY_ID.containsKey(sessionId)) {
			throw new RuntimeException("Unknown session id " + sessionId);
		}

		LOG.info("Session removed: \n{}", SESSION_BY_ID.get(sessionId));

		SESSION_BY_ID.remove(sessionId);
	}
	
} 
