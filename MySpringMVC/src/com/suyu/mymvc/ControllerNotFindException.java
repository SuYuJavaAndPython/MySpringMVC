package com.suyu.mymvc;

/**
 * 用户的类名（请求名）写错了
 */
public class ControllerNotFindException extends RuntimeException{

    public ControllerNotFindException() {}

    public ControllerNotFindException(String message) {
        super(message);
    }
}
