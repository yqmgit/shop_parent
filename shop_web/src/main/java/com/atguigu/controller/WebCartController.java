package com.atguigu.controller;

import com.atguigu.client.CartFeignClient;
import com.atguigu.client.ProductFeignClient;
import com.atguigu.entity.SkuInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

@Controller
public class WebCartController {

    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private CartFeignClient cartFeignClient;

    @GetMapping("addCart.html")
    public String addCart(@RequestParam Long skuId,@RequestParam Long skuNum, HttpServletRequest request){

        //远程调用shop-cart把数据添加到数据库中
        cartFeignClient.addToCart(skuId,skuNum);
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("skuNum",skuNum);
        return "cart/addCart";
    }

    @GetMapping("cart.html")
    public String cartList(){
        return "cart/index";
    }
}
