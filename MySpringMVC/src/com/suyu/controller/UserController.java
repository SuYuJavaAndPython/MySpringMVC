package com.suyu.controller;

import com.suyu.mymvc.ModelAndView;
import com.suyu.mymvc.RequestMapping;
import com.suyu.mymvc.RequestParam;
import com.suyu.mymvc.SessionAttributes;
import com.suyu.service.UserService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@SessionAttributes("name")
public class UserController extends HttpServlet {

    private UserService userService = new UserService();

    /**
     * 原来是具体做事的，现在有了两个小弟login register
     * 这个类的service方法就升级为小总管了
     * 负责分发请求(反射)
     * 小知识总结：
     *         URL统一资源定位器全路径（不包括？携带的参数）    http://ip:port/项目名/资源名
     *         URI统一资源标识符（同样不包括？携带的参数）      /项目名/资源名
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
//    @Override
//    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        //分发的过程
//        //1.获取请求资源名(与下面的某个小弟方法名一致)   eg: /MySpringMVC/login按/拆分数组第一个元素是""空串
//        String uri = request.getRequestURI();
//        //按/拆分不好之处在于有可能有好几个/含包名
////        String methodName = uri.split("/")[2];
//        String methodName = uri.substring(uri.lastIndexOf("/")+1);
//        try {
//            //2.根据方法名反射执行下面某个小弟对应的方法
//            //  设计规约----约定优于配置 (用我的框架就要遵循我的规则，用户写的小弟方法名必须要和请求名一致)
//            //想要通过反射找方法需要先找方法对应的类 Class.forName(类全名)   obj.getClass()  类名.Class
//            Class clazz = this.getClass();
//            Method method = clazz.getDeclaredMethod(methodName,HttpServletRequest.class,HttpServletResponse.class);
//            //3.让方法执行invoke
//            method.invoke(this,request,response);
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        }
//    }

    /**
     * 一个方法代表一个功能：
     *   eg：此方法代表用户的登录功能
     * @param name
     * @param pass
     * @return
     */
    @RequestMapping("login.do")
    public ModelAndView login(@RequestParam("name") String name, @RequestParam("pass") String pass) {
        //2.接收请求携带的参数
//        String name = request.getParameter("name");
//        String pass = request.getParameter("pass");
        //3.调用业务层的方法处理真实业务
        System.out.println("请求到达控制层" + name + ":" + pass);
        String result = userService.login(name,pass);
        //创建一个ModelAndView对象包装我们需要给框架的内容
        ModelAndView mv = new ModelAndView();
        if("登录成功".equals(result)){
            mv.addAttribute("name",name);
            //4.将响应资源的路径告知大总管
            mv.setViewName("welcome.jsp");
        }else{
            mv.addAttribute("result",result);
            mv.setViewName("index.jsp");
        }
        return mv;
    }

    /**
     * 此方法代表用户的注册功能
     * @param request
     * @throws ServletException
     * @throws IOException
     */
    public String register(HttpServletRequest request) {
        //流程与login方法大致一致
        // 0.处理字符集
        // 1.接收请求携带的数据
        // 2.处理数据（可以没有）
        // 3.调用业务层方法通常会的到一个结果
        // 4.控制层将响应信息（即这个结果）响应回浏览器
        System.out.println("我是UserController控制层的register方法，我执行啦");
        return "xxx.jsp";
    }
}
