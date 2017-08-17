package com.titizz.shorturl.web.controller;

import com.titizz.shorturl.exception.NoQueryResultReturnException;
import com.titizz.shorturl.service.ShortUrlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;

/**
 * Created by code4wt on 17/8/7.
 */
@Controller
public class IndexController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ShortUrlService service;

    @RequestMapping(value = {"/", "index", "index.htm", "index.html", "home"})
    public String index() {
        return "index";
    }

    @RequestMapping(value = "404")
    public String notFound() {
        return "404";
    }

    @RequestMapping(value = "/{base62:[0-9a-zA-Z]+$}", method = RequestMethod.GET)
    public String redirect(
            @PathVariable
            String base62) throws IOException {
        String originUrl = null;
        try {
            originUrl = service.decompress(base62);
        } catch (NoQueryResultReturnException e) {
            logger.info("no redirect target correspond with base62 {}, forward 404", base62);
            return "404";
        }

        logger.info("base62 {} will be redirected {}", base62, originUrl);

        return "redirect:" + originUrl;
    }
}
