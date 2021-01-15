<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<html>
  <head>
    <title>MySpringMVC</title>
  </head>
  <body>

    <!--
      发送请求时携带三个参数
      请求类名
      请求类型.do
      请求方法名
    -->
    ${requestScope.result}
<%--    用户一类的功能点--%>
    <a href="login.do?name=zzt&pass=123">测试1（模拟一个商城用户的登录功能）</a><br>
    <a href="UserController.do?method=register">测试2（模拟一个商城用户的注册功能）</a><br>

<%--    商品种类查询的一类功能点--%>
    <a href="KindController.do?method=kind">测试3（模拟一个商城用户的查询商品种类功能）</a><br>
    <a href="KindController.do?method=goods">测试1（模拟一个商城用户的查询具体商品功能）</a><br>
  </body>
</html>
