package com.covisint.platform.gateway.discovery;

import java.io.StringReader;
import java.util.Locale;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.ifaces.AllSeenIntrospectable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class Introspector {

	private static final String DOCTYPE_HEADER = "<!DOCTYPE node PUBLIC .*\\\"\\s*\\\".*\\.dtd?\">";

	private static final String PREFERRED_LANG = Locale.getDefault().getLanguage();

	private static final Logger LOG = LoggerFactory.getLogger(Introspector.class);

	public IntrospectResult doIntrospection(AllSeenIntrospectable input) {

		try {
			String[] langs = input.GetDescriptionLanguages();
			boolean enFound = false;

			if (langs != null && langs.length > 0) {
				for (final String lang : langs) {
					if (PREFERRED_LANG.equalsIgnoreCase(lang)) {
						enFound = true;
						break;
					}
				}
			}

			if (!enFound) {
				LOG.info("Introspect language '{}' not available.  Using default.", PREFERRED_LANG);
			}

			String xml = input.IntrospectWithDescription(PREFERRED_LANG);

			xml = xml.replaceFirst(DOCTYPE_HEADER, "");
			
			LOG.debug("=======================");
			LOG.debug("|| Introspection XML ||");
			LOG.debug("=======================");
			LOG.debug(xml);

			JAXBContext jaxbContext = JAXBContext.newInstance(IntrospectResult.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

			return (IntrospectResult) jaxbUnmarshaller.unmarshal(new StringReader(xml));

		} catch (BusException | JAXBException e) {
			throw new RuntimeException(e);
		}

	}

}
