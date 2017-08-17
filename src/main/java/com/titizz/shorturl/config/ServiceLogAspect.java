package com.titizz.shorturl.config;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Created by code4wt on 17/8/12.
 */
@Aspect
@Component
public class ServiceLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(ServiceLogAspect.class);

    @Pointcut("execution(* com.titizz.shorturl.service.InitialCodeService.updateInitialCode())")
    public void updateInitialCodePointcut() {}

    @AfterReturning(returning = "initialCode", pointcut = "updateInitialCodePointcut()")
    public void updateInitialCodeAfterReturning(Object initialCode) {
        logger.info("get a new initial code {}", initialCode);
    }

    @AfterThrowing(throwing = "t", pointcut = "updateInitialCodePointcut()")
    public void updateInitialCodeAfterThrowing(Throwable t) {
        logger.warn("", t);
    }
}
