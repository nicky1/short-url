package com.titizz.shorturl.config;

import com.titizz.shorturl.UrlUtils;
import com.titizz.shorturl.web.vo.ResponseMessage;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;

/**
 * Created by code4wt on 17/8/13.
 */
@Aspect
@Component
public class ParameterPretreatmentAspect {

    @Around("execution(* com.titizz.shorturl.web.controller.ShortUrlController.compress(String))")
    public Object pretreatUrl(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String url = (String) args[0];

        try {
            if (!UrlUtils.validateUrl(url)) {
                throw new MalformedURLException();
            }
            url = UrlUtils.normalizeUrl(url);
            return joinPoint.proceed(new Object[]{url});
        } catch (MalformedURLException e) {
            return new ResponseMessage("", url, false, "url is not valid");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return new ResponseMessage("", url, false, "unknown error");
        }
    }
}
