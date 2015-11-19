package com.covisint.platform.gateway;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO fix, this isn't capturing.
@Aspect
public class BusSignalHandlerAspect {

	private static final Logger LOG = LoggerFactory.getLogger(BusSignalHandlerAspect.class);

	@Around("execution(@org.alljoyn.bus.annotation.BusSignalHandler * *(..))")
	public Object doBasicProfiling(ProceedingJoinPoint pjp) throws Throwable {

		LOG.info("captured bus signal handler aspect.");

		Object retVal = pjp.proceed();

		LOG.info("Executed bus signal handler method and returning now.");

		return retVal;
	}

	@Around("execution(@org.springframework.web.bind.annotation.RequestMapping * *(..))")
	public Object requestMapped(ProceedingJoinPoint pjp) throws Throwable {
		return pjp.proceed();
	}

}
