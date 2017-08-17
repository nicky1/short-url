package com.titizz.shorturl.config;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by code4wt on 17/8/12.
 */
@Aspect
@Component
public class ControllerLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(ControllerLogAspect.class);

    @Pointcut("execution(* com.titizz.shorturl.web.controller.*Controller.*(..))" +
            "&& @annotation(org.springframework.web.bind.annotation.RequestMapping))")
    public void requestRecord() {}

    @Before("requestRecord()")
    public void record() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String ip = request.getHeader("X-Real-IP");
        String path = request.getServletPath();
        logger.info("{} access {}", ip, path);
    }
}
