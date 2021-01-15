package com.suyu.mymvc;

public class ParameterTypeNotSupportException extends RuntimeException {

    public ParameterTypeNotSupportException() {}

    public ParameterTypeNotSupportException(String message) {
        super(message);
    }
}
