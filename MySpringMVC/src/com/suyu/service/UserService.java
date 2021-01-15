package com.suyu.service;

public class UserService {

    /**
     * 设计一个假的登录方法
     * @param name
     * @param pass
     * @return
     */
    public String login(String name, String pass){
        if("zzt".equals(name) && "123".equals(pass)){
            return "登录成功";
        }
        return "用户名或密码错误";
    }
}
