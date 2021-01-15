package com.suyu.mymvc;

public class LackOfConfigurationException extends RuntimeException {

    public LackOfConfigurationException() {}

    public LackOfConfigurationException(String message) {
        super(message);
    }
}
