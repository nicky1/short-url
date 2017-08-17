package com.titizz.shorturl.web.controller;

import com.titizz.shorturl.exception.NoQueryResultReturnException;
import com.titizz.shorturl.service.ShortUrlService;
import com.titizz.shorturl.web.vo.ResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Size;
import java.io.IOException;

@RestController
@Validated
public class ShortUrlController {

    private static final Logger logger = LoggerFactory.getLogger(ShortUrlController.class);

    @Autowired
	private ShortUrlService service;
	
	@RequestMapping(value = "compress")
	public ResponseMessage compress(
			@RequestParam("url")
            @Size(min = 20, max = 300, message = "url 长度不符合规定")
			String url) {

        String shortUrl = null;
        try {
            shortUrl = service.compress(url);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("failed in compressing, unknown error");
            return new ResponseMessage(shortUrl, url, false, "url is not valid");
        }

		return new ResponseMessage(shortUrl, url, true, "succeed in compressing");
	}
	
	@RequestMapping(value="decompress")
	public ResponseMessage decompress(@RequestParam("code") String code) throws IOException {

		String url = null;
		try {
			url = service.decompress(code);
		} catch (NoQueryResultReturnException e) {
		    String message = "failed in decompressing, no url correspond whth code " + code;
		    logger.info(message);
			return new ResponseMessage(url, false, message, code);
		}

		logger.info("success in decompressing, url {} correspond whth code {}", url, code);
		return new ResponseMessage(url, true, "success in decompressing", code);
	}
}
