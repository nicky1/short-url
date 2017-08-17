package com.titizz.shorturl.service;

import com.titizz.shorturl.exception.FailToAcquireDistributeLockException;
import com.titizz.shorturl.exception.NoMoreInitialCodeException;

/**
 * Created by code4wt on 17/8/2.
 */
public interface InitialCodeService {

    Long updateInitialCode() throws NoMoreInitialCodeException,
            InterruptedException, FailToAcquireDistributeLockException;
}
