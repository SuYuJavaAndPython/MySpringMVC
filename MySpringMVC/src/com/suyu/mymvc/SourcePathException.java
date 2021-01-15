package com.suyu.mymvc;

/**
 * 用户传递"" 或者 null给框架处理响应时报异常告知用户
 */
public class SourcePathException extends RuntimeException {

    public SourcePathException() {}

    public SourcePathException(String message) {
        super(message);
    }
}
