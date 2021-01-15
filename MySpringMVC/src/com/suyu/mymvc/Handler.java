package com.suyu.mymvc;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigInteger;
import java.net.URL;
import java.util.*;

public class Handler {

    //属性 ---- 存储请求名（类名.do）如：UserController.do与类全名的映射关系
    //  请求名与类全名需要在DispatcherServlet中service方法执行之前加载完毕
    //  故加载这件事可以在如下地方写：
    //      1.块 （或静态块，相应的集合也要变静态的）
    //      2.构造方法
    //          Tomcat帮我们管理Servlet是单例的且是延迟加载的，当用到这个servlet时才会调用构造方法创建servlet对象
    //          即便如此构造方法也会在service方法执行之前执行，因为service方法的执行是由servlet对象调用的
    //      3.Tomcat帮我们管理servlet，那么对应的servlet有三个表示生命周期的方法
    //          init  service  destroy
    //          其中init方法表示servlet对象初始化时调用的
    private Map<String,String> classMappingMap = new HashMap<>();
    //属性 ---- 存储类名（请求名）与类对应的对象的对应关系
    //  每一个Controller对象都是单例的且是延迟加载的机制
    private Map<String,Object> classObjMappingMap = new HashMap<>();
    //属性 ---- 存储对象与某个Controller类中的所有方法集合（方法名与方法）
    private Map<Object,Map<String,Method>> objectMethodMap = new HashMap<>();
    //属性 ---- 当用户使用的请求形式为login.do?name=zzt&pass=123时
    //      充当原来classMappingMap集合的作用，存储请求名login.do与类全名的对应关系
    private Map<String,String> methodClassMappingMap = new HashMap<>();

    //=======================================================================================
    //以下方法统一用默认不写修饰符保证封装性

    /**
     * 0号小弟：
     *  负责加载ApplicationContext.properties文件中的内容到classMappingMap集合里
     * @return
     */
    boolean loadProperties(){
        //变量flag为true时表示用户使用了配置文件 false表示用户使用注解
        boolean flag = true;
        try {
            Properties pro = new Properties();
            //用户可能没用配置文件，故文件名"ApplicationContext.properties"不存在，即is为空
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("ApplicationContext.properties");
            pro.load(is);
            Enumeration en = pro.propertyNames();
            while(en.hasMoreElements()){
                String className = (String) en.nextElement();
                String classAllName = pro.getProperty(className);
                classMappingMap.put(className,classAllName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e){
            flag = false;
        }
        return flag;
    }

    /**
     * 0号小弟:
     *  负责在init方法执行的时候 扫描类中的注解 请求--类--方法 对应关系
     * @param packageNames
     */
    void scanAnnotation(String packageNames){
        //解析包名如: com.suyu.controller,com.suyu.dao
        //分析用户需要扫的包都有哪些
        String[] packages = packageNames.split(",");
        //挨个去找每个包下的所有Controller.class文件
        for(String packageName : packages){
            //解析包名如：com.suyu.controller     \\转义    com\suyu\controller
            String realPackageName = packageName.replace(".","\\");
            URL url = Thread.currentThread().getContextClassLoader().getResource(realPackageName);
            //判断用户写的包名是否正确
            if(url == null){
                throw new LackOfConfigurationException("web.xml information is mistake");
            }
            //获取绝对路径/E:/IdeaProjects/MySpringMVC/out/artifacts/MySpringMVC_war_exploded/WEB-INF/classes/com/suyu/controller/
            String fullPackageName = url.getPath();
            //packageFile代表的是controller文件夹的真身
            File packageFile = new File(fullPackageName);
            //获得该文件夹下的所有以.class为后缀的文件，如果里面还有文件那么处理不了
            File[] files = packageFile.listFiles(file -> {
                    if(file.isFile() && file.getName().endsWith(".class")){
                        return true;
                    }
                    return false;
            });
            //得到所有Controller.class文件
            for(File file : files){
                //获取简单的Controller类名
                String simpleName = file.getName();
                //获取类全名 包名加简单类名com.suyu.controller+simpleName
                String classFullName = packageName + "." + simpleName.substring(0,simpleName.indexOf("."));
                try {
                    //反射获得类
                    Class clazz = Class.forName(classFullName);
                    //看看类上是否有注解
                    RequestMapping classAnnotation = (RequestMapping) clazz.getAnnotation(RequestMapping.class);
                    if(classAnnotation != null){
                        //获取注解携带信息如："UserController.do"
                        String[] values = classAnnotation.value();
                        for(String value : values){
                            //类上那个RequestMapping注解携带的信息与类全名的对应关系
                            classMappingMap.put(value,classFullName);
                        }
                    }
                    //获取Controller类中的所有方法
                    Method[] methods = clazz.getDeclaredMethods();
                    for(Method method : methods){
                        RequestMapping methodAnnotation = method.getAnnotation(RequestMapping.class);
                        if(methodAnnotation != null) {
                            //获取注解携带信息如："login.do"
                            String[] values = methodAnnotation.value();
                            for (String value : values) {
                                //类上那个RequestMapping注解携带的信息与类全名的对应关系
                                methodClassMappingMap.put(value, classFullName);
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * 1号小弟：
     *  负责解析URI得到请求名（类名.do）
     * @param uri
     * @return
     */
    String parseURI(String uri){
        return uri.substring(uri.lastIndexOf("/")+1);
    }

    /**
     * 2号小弟：
     *  负责得对象以便后面的方法执行
     *  注意requestContent可以是UserController.do    也可以是login.do
     * @param requestContent
     * @return
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    Object findObject(String requestContent) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        //上集合中找类名对应的那个对象
        Object obj = classObjMappingMap.get(requestContent);
        //如果对象为空，我们就创建一个对象（延迟加载机制+单例机制）
        if(obj == null){
            //获取类全名来创建class类，然后才能创建对象
            //根据classMappingMap集合获取类名对应的那个类全名，然后再通过类全名获取其对应的Class类
            String classFullName = classMappingMap.get(requestContent);
            if(classFullName == null){
                classFullName = methodClassMappingMap.get(requestContent);
                if(classFullName == null) {
                    //用户的请求名写错了，抛异常告知用户
                    throw new ControllerNotFindException(requestContent + " is not exist");
                }
            }
            Class clazz = Class.forName(classFullName);
            //双重检测模式保证Controller对象的单例
            synchronized (clazz){
                if(obj == null) {
                    //这个过程本质上是调用该类中的无参构造方法
                    obj = clazz.newInstance();
                }
            }
            //注意此处用clazz.newInstance()方法在jdk11中是不建议使用的
            //因为有可能该类中没有无参的构造方法
            //更建议用clazz：
//            Constructor constructor = clazz.getConstructor();
//            constructor.newInstance();
            //装入集合中保证下一次访问集合中用的对象是同一个
            classObjMappingMap.put(requestContent,obj);
            //获取某个Controller类中的所有方法
            Method[] methods = clazz.getDeclaredMethods();
            //创建一个map用于存储所有的方法
            Map<String,Method> methodMap = new HashMap<>();
            for(Method method : methods){
                methodMap.put(method.getName(),method);
            }
            objectMethodMap.put(obj,methodMap);
        }
        return obj;
    }

    /**
     * 3号小弟：
     *  负责得到对应的方法
     * @param obj
     * @param methodName
     * @return
     * @throws NoSuchMethodException
     */
    Method findMethod(Object obj, String methodName) {
        //现在：
        Map<String,Method> methodMap = objectMethodMap.get(obj);
        Method method = methodMap.get(methodName);
        if(method == null){
            //通过方法名反射获取方法时没找到方法 可能用户写请求时方法名写错了
            throw new MethodNotFindException(methodName + " is not exist");
        }
        return method;
        //原来：
//        Class clazz = obj.getClass();
//        Method method = clazz.getMethod(methodName, HttpServletRequest.class);
//        return method;
    }

//    /**
//     * 4号小弟：
//     *  负责处理响应信息
//     * @param result
//     * @param request
//     * @param response
//     */
//    void handleResponse(String result, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        request.getRequestDispatcher(result).forward(request,response);
//    }

    /**
     * 4.1号小弟：
     *  负责解析mv对象，将其中的map集合中的key-value存入request域中
     * @param obj
     * @param mv
     * @param request
     */
    private void parseModelAndView(Object obj, ModelAndView mv, HttpServletRequest request){
        //获取装值的集合
        Map<String,Object> attributeMap = mv.getAttributeMap();
        //挨个将值装入request中
        Iterator it = attributeMap.keySet().iterator();
        while(it.hasNext()){
            String key = (String) it.next();
            Object value = attributeMap.get(key);
            //将这一组key-value存入request域中
            request.setAttribute(key,value);
        }
        //判断当前Controller类上是否有@SessionAttributes
        SessionAttributes sessionAttributes = obj.getClass().getAnnotation(SessionAttributes.class);
        if(sessionAttributes != null){
            String[] attributeKeys = sessionAttributes.value();
            if(attributeKeys != null && attributeKeys.length > 0) {
                for (String attributeKey : attributeKeys) {
                    String attributeValue = (String) mv.getAttribute(attributeKey);
                    //判断需要存入的信息是否成功存入
                    if (attributeValue != null) {
                        HttpSession session = request.getSession();
                        session.setAttribute(attributeKey, attributeValue);
                    }
                }
            }
        }
    }

    /**
     * 4.2号小弟：
     *  负责解析String做响应
     * @param methodResult
     * @param method
     * @param request
     * @param response
     */
    private void parseString(String methodResult, Method method, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        //严谨判断  如用户返回值给的是 null  ""
        if("".equals(methodResult) || methodResult == null){
            throw new SourcePathException(methodResult + " is not normal");
        }
        //先看一看方法上面是否有注解
        ResponseBody responseBody = method.getAnnotation(ResponseBody.class);
        //方法上有注解做标记     可以直接给予响应
        if(responseBody != null){
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(methodResult);
        }else{      //方法上没有注解   是某个资源路径"index.jsp" "forward:index.jsp" "redirect:index.jsp"
            String[] path = methodResult.split(":");
            //是默认转发
            if(path.length == 1){
                request.getRequestDispatcher(methodResult).forward(request,response);
            } else if("forward".equals(path[0].trim())){
                //是加forward说明的转发
                request.getRequestDispatcher(path[1].trim()).forward(request,response);
            } else if("redirect".equals(path[0].trim())){
                //是加redirect说明的重定向
                response.sendRedirect(path[1].trim());
            }
        }
    }

    /**
     * 4.3号小弟：
     *  负责将对象转化为JSON格式再响应回浏览器
     * @param methodResult
     * @param method
     * @param response
     */
    private void parseObjectsTransformJSON(Object methodResult, Method method, HttpServletResponse response) throws IOException {
        //判断是否有注解
        ResponseBody responseBody = method.getAnnotation(ResponseBody.class);
        if(responseBody != null){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("jsonObject",methodResult);
            response.getWriter().write(jsonObject.toJSONString());
        }else{
            //缺少注解也返回了对象
            throw new ObjectNotTransformJSONException("lack of annotation about @ResponseBody");
        }
    }

    /**
     * 大4号小弟
     * @param obj       需要用来判断当前Controller类上是否有@SessionAttributes，有则需要将某个键值存入session中
     * @param methodResult    需要解析这个返回值是什么再做具体的事
     * @param method    需要判断方法是否加了ResponseBody注解做标记说明
     * @param request   可能需要request存值setAttribute或者直接响应或者转发
     * @param response  可能需要response重定向
     */
    void finalHandleResponse(Object obj, Object methodResult, Method method, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        //证明用户需要框架帮忙做响应
        if(methodResult != null){
            //返回的是ModelAndView类型
            if(methodResult instanceof ModelAndView){
                //需要小弟来解析ModelAndView对象中需要存入request域中的值
                ModelAndView mv = (ModelAndView) methodResult;
                this.parseModelAndView(obj, mv, request);
                //需要小弟来解析mv对象中的String字符串（为了下面单独是String类型的情况）分了两个小弟方法写，减少代码冗余
                this.parseString(mv.getViewName(), method, request, response);
            }else if(methodResult instanceof String){   //返回值是String类型
                this.parseString((String) methodResult, method, request, response);
            }else{  //返回值是对象类型
                //需要找小弟组成JSON格式再传给前端
                //解释为什么要加注解才能变JSON：
                //      因为这种情况类似于String加注解代表直接响应
                //      这种情况只不过多了一个JSON转化的过程
                //      可以认为两者都是直接给予浏览器响应response.getWriter().write("xxx");
                //      而ResponseBody注解的作用就是为了表示该响应可以直接进行
                //      所以用注解更能体现这个直接给予响应的过程（不用注解其实也行，只是没这么直观而已）
                this.parseObjectsTransformJSON(methodResult, method, response);
            }
        }else{
            //用户自己做了响应这件事，框架不用干活了
            return;
        }
    }

    /**
     * 5.1号小弟：
     *  负责将请求携带的参数值按方法参数类型注入返回值Object交给5号大哥
     * @param parameterClazz
     * @param paramAnnotation
     * @param request
     * @return
     */
    private Object injectionNormal(Class parameterClazz, RequestParam paramAnnotation, HttpServletRequest request){
        Object result = null;
        //获取注解信息（请求参数名）
        String key = paramAnnotation.value();
        //获取请求参数值
        String value = request.getParameter(key);
        //判断参数类型，按参数类型来注入Object
        if(parameterClazz == int.class || parameterClazz == Integer.class){
            result = new Integer(value);
        }else if(parameterClazz == float.class || parameterClazz == Float.class){
            result = new Float(value);
        }else if(parameterClazz == double.class || parameterClazz == Double.class){
            result = new Double(value);
        }else if(parameterClazz == boolean.class || parameterClazz == Boolean.class){
            result = new Boolean(value);
        }else if(parameterClazz == String.class){
            result = value;
        }else{
            //不是框架支持的普通类型如char short BigInteger......
            throw new ParameterTypeNotSupportException(parameterClazz + "is not supported");
        }
        return result;
    }

    /**
     * 5.2号小弟：
     *  负责将请求携带的参数值按Map类型注入返回值Map交给5号大哥
     * @param obj
     * @param request
     * @return
     */
    private Map injectionMap(Object obj, HttpServletRequest request){
        Map map = (Map) obj;
        //获取请求携带参数的所有键
        Enumeration keys = request.getParameterNames();
        while(keys.hasMoreElements()){
            String key = (String) keys.nextElement();
            String value = request.getParameter(key);
            map.put(key,value);
        }
        return map;
    }

    /**
     * 5.3号小弟：
     *  负责将请求携带的参数值按domain对象的属性类型通过set方法注入obj对象的属性中，将obj交给5号大哥
     * @param obj
     * @param parameterClazz
     * @param request
     * @return
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     */
    private Object injectionDomain(Object obj, Class parameterClazz, HttpServletRequest request) throws IllegalAccessException, InstantiationException {
        //反射获取domain对象的所有方法
        Method[] methods = parameterClazz.getDeclaredMethods();
        for (Method method : methods){
            //找到set方法
            String methodName = method.getName();
            if(methodName.startsWith("set")){
                try {
                    //拼接属性名
                    String fieldName = "";
                    if(methodName.length() > 4){
                        fieldName = methodName.substring(3,4).toLowerCase() + methodName.substring(4);
                    }else{
                        //属性名就一个字符
                        fieldName = methodName.substring(3).toLowerCase();
                    }
                    //通过参数键获取值（属性名与参数键一一对应）
                    String value = request.getParameter(fieldName);
                    //获取属性set方法的参数类型（我们认为用户不会自己搞自己set方法写俩参数）
                    Class setParamClazz = method.getParameterTypes()[0];
                    //注意这个setParamClazz是对象某个属性的类型
                    //  1.这个类型可以是普通类型（下面的几行代码即可）
                    //设置set方法执行时需要的值为setParamValue
                    Object setParamValue = null;
                    //获取其参数类型String int Integer......
                    // 注意包装类Character没有带String参数的构造方法，故加个判断
                    if(setParamClazz == char.class || setParamClazz == Character.class){
                        //请求携带的值是单个字符
                        setParamValue = value.toCharArray()[0];
                    } else if(setParamClazz == String.class
                            || setParamClazz == int.class || setParamClazz == Integer.class
                            || setParamClazz == float.class || setParamClazz == Float.class
                            || setParamClazz == double.class || setParamClazz == Double.class
                    ){
                        Constructor constructor = setParamClazz.getConstructor(String.class);
                        //利用包装类的带String参数的构造方法构造成set参数类型
                        setParamValue = constructor.newInstance(value);
                    } else{
                        //  2.这个类型可以是一个对象（需要递归）
                        setParamValue = this.injectionDomain(setParamValue, setParamClazz, request);
                    }
                    method.invoke(obj, setParamValue);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        return obj;
    }

    /**
     * 5号小弟:
     *  负责用request接收请求发送过来的参数根据方法的参数列表存入Object[]中
     *  把这些个参数存入到一个Object[] 交给后面的方法执行时候使用
     *  是否需要参数    Method(分析方法的参数 参数回头找请求是否有给我传递过来) request?
     *                  request，response用户写的方法可能还需要request或者response
     *  是否需要返回值  Object[]
     * @param method
     * @param request
     * @param response
     * @return
     */
    Object[] injectionParameters(Method method , HttpServletRequest request, HttpServletResponse response) throws IllegalAccessException, InstantiationException {
        //获取方法上所有的参数
        Parameter[] parameters = method.getParameters();
        //如果方法没有参数则返回null即可
        if(parameters == null || parameters.length == 0){
            return null;
        }
        //证明方法是有参数的  肯定需要一个Object[]数组 留着用来装最终的返回值
        //resultValue长度与方法参数数量一致（一一对应）
        Object[] resultValue = new Object[parameters.length];
        //循环挨个分析每一个参数的类型以便将请求携带的参数对应注入
        //这个类型通过框架约定为：
        //  1.携带注解的普通类型String int Integer......
        //  2.domain实体对象
        //  3.Map集合
        //  4.request
        //  5.response
        //其他类型框架暂时无法处理，抛异常告知用户
        for(int i = 0; i < parameters.length; i++){
            Parameter parameter = parameters[i];
            //获取当前参数的class类
            Class parameterClazz = parameter.getType();
            //获取当前参数携带的注解
            RequestParam paramAnnotation = parameter.getAnnotation(RequestParam.class);
            //判断当前参数是否携带了注解的普通参数String int Integer......
            if(paramAnnotation != null){
                //是携带了注解的普通参数，找个小小弟来负责存入值
                resultValue[i] = this.injectionNormal(parameterClazz, paramAnnotation, request);
            }else{
                if(parameterClazz.isArray() || parameterClazz == List.class || parameterClazz == Set.class){
                    //如果参数不是框架支持的任何一种，那么框架处理不了
                    //不支持的情况暂时先这么写，其实这样写不严谨也不恰当
                    throw new ParameterTypeNotSupportException(parameterClazz + "is not supported");
                }else {
                    //参数是domain Map request response
                    if (parameterClazz == HttpServletRequest.class) {
                        //参数是request
                        resultValue[i] = request;
                        continue;
                    }
                    if (parameterClazz == HttpServletResponse.class) {
                        //参数是response
                        resultValue[i] = response;
                        continue;
                    }
                    //参数是domain map
                    Object obj = parameterClazz.newInstance();
                    if (obj instanceof Map) {
                        //参数是map，找个小小弟来负责存入值
                        resultValue[i] = this.injectionMap(obj, request);
                    } else {
                        //参数是domain，找个小小弟来负责存入值
                        resultValue[i] = this.injectionDomain(obj, parameterClazz, request);
                    }
                }
            }
        }
        return resultValue;
    }
}
