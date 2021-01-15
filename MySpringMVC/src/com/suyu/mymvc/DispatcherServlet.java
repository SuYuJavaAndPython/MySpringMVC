package com.suyu.mymvc;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * dispatcher英文意思为：调度员
 * 这个类是大总管，负责通过反射分发各种servlet请求
 */
public class DispatcherServlet extends HttpServlet {

    //DispatcherServlet的小弟类，这样实现自己类中只干自己负责的事情，方便找寻管理
    private Handler handler = new Handler();

    /**
     * 在init方法中写更能体现自己对Tomcat管理servlet机制的理解
     * @param config
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        boolean flag = handler.loadProperties();
        //判断用户是否使用了配置文件
        if(!flag){  //没使用配置文件
            String packageNames = config.getInitParameter("scanPackage");
            if(packageNames == null){   //没使用注解或使用了注解却没配置找注解需要的包名信息
                throw new LackOfConfigurationException("web.xml lack of information or lack of configuration file");
            }else{  //有包名信息，可以去扫包得类全名了
                handler.scanAnnotation(packageNames);
            }
        }
    }

    /**
     * 注意这个service方法的流程目的：
     *  找类名（请求名）和方法名 (service自己做的事情，获取请求携带的信息)
     *  --> 找类名对应的那个类 (1号小弟做的事情，解析uri)
     *  --> 通过类名获取classMappingMap集合类全名来得到类对应的对象 (2号小弟做的事情，得对象以便后面的方法执行)
     *  --> 找类中与方法名一致的那个方法 (3号小弟做的事情，得到对应的方法)
     *  --> 让方法invoke执行 (准备工作已经让小弟们做好了，自己处理最后的执行即可)
     *  --> 处理响应信息 (4号小弟做的事情，处理响应信息如转发，重定向，直接响应，JSON响应......)
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            //0.设置请求携带参数的字符集
            request.setCharacterEncoding("UTF-8");
            //1.获取请求的统一资源标识符uri
            String uri = request.getRequestURI();
            //2.找1号小弟处理获得的uri，解析uri得到类名（请求名）
            String requestContent = handler.parseURI(uri);
            //获取请求携带的方法名
            String methodName = request.getParameter("method");
            if(methodName == null){
                methodName = requestContent.substring(0,requestContent.indexOf("."));
            }
            //3.找2号小弟得类对象
            Object obj = handler.findObject(requestContent);
            //4.找3号小弟得类方法
            Method method = handler.findMethod(obj,methodName);
            //5.找5号小弟做参数注入
            Object[] paramValues = handler.injectionParameters(method,request,response);
            //6.自己做事让method执行
            Object methodResult = method.invoke(obj,paramValues);
            //7.找大4号小弟处理响应信息
            handler.finalHandleResponse(obj,methodResult,method,request,response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
