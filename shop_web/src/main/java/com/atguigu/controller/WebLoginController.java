package com.atguigu.controller;

import com.atguigu.client.ProductFeignClient;
import com.atguigu.client.SearchFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class WebLoginController {
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private SearchFeignClient searchFeignClient;

    //1.实现跳转到登录页面 http://passport.gmall.com/login.html?originalUrl=http://item.gmall.com/
    @GetMapping("login.html")
    public String login(HttpServletRequest request){
        String originalUrl= request.getParameter("originalUrl");
        request.setAttribute("originalUrl",originalUrl);
        //返回到登录页面 省略了html后缀
        return "login";
    }

}