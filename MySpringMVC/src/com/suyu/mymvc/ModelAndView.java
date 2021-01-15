package com.suyu.mymvc;

import java.util.HashMap;
import java.util.Map;

/**
 * 这个类出现的目的是为了包装：
 *  Controller方法执行过程中需要request带走的值
 *  方法执行后所要去的的某个资源的路径
 * 为什么这个类叫ModelAndView
 *    model模型   数据模型(注意跟我们自己的MVC区分  数据 用来存入request作用于带走的  map集合)
 *    view视图    用来转发展示用的(注意跟我们自己的MVC区分  转发路径-->展示的视图层资源  )
 */
public class ModelAndView {

    //属性资源路径
    private String viewName;
    //属性键值对集合map（用于装需要存入request，session域中的键值对）
    private Map<String,Object> attributeMap = new HashMap<>();

    /**
     * 提供给用户使用的将方法执行后所要去的的某个资源的路径存入ModelAndView的对象中
     * @param viewName
     */
    public void setViewName(String viewName){
        this.viewName = viewName;
    }
    /**
     * 提供给用户使用的将 Controller方法执行过程中需要request带走的值存入ModelAndView的对象中
     * @param key
     * @param value
     */
    public void addAttribute(String key, Object value){
        this.attributeMap.put(key, value);
    }

    /**
     * 框架自己用的获得资源路径
     * @return
     */
    String getViewName(){
        return this.viewName;
    }
    /**
     * 框架自己用的获得需要存入request中的值
     * @param key
     * @return
     */
    Object getAttribute(String key){
        return this.attributeMap.get(key);
    }
    /**
     * 保留一个框架自己用的获得这个装值的集合，框架自己挨个遍历
     * @return
     */
    Map<String,Object> getAttributeMap(){
        return this.attributeMap;
    }
}
