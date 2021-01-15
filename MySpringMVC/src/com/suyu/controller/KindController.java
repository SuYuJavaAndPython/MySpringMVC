package com.suyu.controller;

import javax.servlet.http.HttpServletRequest;

public class KindController {

    public String kind(HttpServletRequest request) {
        System.out.println("我是KindController中的kind方法，我执行啦");
        return "xxx.jsp";
    }

    public String goods(HttpServletRequest request) {
        System.out.println("我是KindController中的goods方法，我执行啦");
        return "xxx.jsp";
    }
}
