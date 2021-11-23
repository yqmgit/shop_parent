package com.atguigu.controller;

import com.atguigu.client.OrderFeignClient;
import com.atguigu.entity.OrderInfo;
import com.atguigu.result.RetVal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
public class WebPayController {

    @Autowired
    private OrderFeignClient orderFeignClient;

    //跳转到支付页面
    @GetMapping("pay.html")
    public String confirm(@RequestParam Long orderId, Model model){
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        model.addAttribute("orderInfo",orderInfo);
        return "payment/pay";
    }
    //http://payment.gmall.com/alipay/success.html
    //支付成功后跳转到支付成功页面  同步通知
    @GetMapping("alipay/success.html")
    public String success(){
        return "payment/success";
    }
}
