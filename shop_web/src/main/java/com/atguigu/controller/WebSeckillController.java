package com.atguigu.controller;

import com.atguigu.client.OrderFeignClient;
import com.atguigu.result.RetVal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
public class WebSeckillController {


    //跳转到秒杀页面
    @GetMapping("seckill-index.html")
    public String seckill(){

        return "seckill/index";
    }
}
