package com.titizz.shorturl.service;

import com.titizz.shorturl.exception.FailToAcquireDistributeLockException;
import com.titizz.shorturl.exception.NoMoreInitialCodeException;
import com.titizz.shorturl.exception.NoQueryResultReturnException;

public interface ShortUrlService {

    String compress(String url) throws InterruptedException,
            NoMoreInitialCodeException, FailToAcquireDistributeLockException;

	String decompress(String base62) throws NoQueryResultReturnException;
}
