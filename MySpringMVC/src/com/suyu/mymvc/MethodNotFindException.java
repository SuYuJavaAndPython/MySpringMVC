package com.suyu.mymvc;

/**
 * 通过方法名反射获取方法时没找到方法
 */
public class MethodNotFindException extends RuntimeException {

    public MethodNotFindException() {}

    public MethodNotFindException(String message) {
        super(message);
    }
}
