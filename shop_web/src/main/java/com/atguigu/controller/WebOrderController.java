package com.atguigu.controller;

import com.atguigu.client.CartFeignClient;
import com.atguigu.client.OrderFeignClient;
import com.atguigu.client.ProductFeignClient;
import com.atguigu.entity.SkuInfo;
import com.atguigu.result.RetVal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Controller
public class WebOrderController {

    @Autowired
    private OrderFeignClient orderFeignClient;

    //跳转到订单确认页面
    @GetMapping("confirm.html")
    public String confirm(Model model){
        RetVal<Map<String,Object>> confirm = orderFeignClient.confirm();
        model.addAllAttributes(confirm.getData());
        return "order/confirm";
    }
}
