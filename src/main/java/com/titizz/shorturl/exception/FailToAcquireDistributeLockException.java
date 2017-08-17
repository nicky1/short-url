package com.titizz.shorturl.exception;

/**
 * Created by code4wt on 17/8/6.
 */
public class FailToAcquireDistributeLockException extends Exception {

    public FailToAcquireDistributeLockException() {}

    public FailToAcquireDistributeLockException(String message) {
        super(message);
    }
}
