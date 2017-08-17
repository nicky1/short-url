package com.titizz.shorturl.exception;

/**
 * Created by code4wt on 17/8/2.
 */
public class NoMoreInitialCodeException extends Exception {

    public NoMoreInitialCodeException() {}

    public NoMoreInitialCodeException(String message) {
        super(message);
    }
}
