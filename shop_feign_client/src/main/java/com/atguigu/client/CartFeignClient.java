package com.atguigu.client;


import com.atguigu.entity.*;
import com.atguigu.fallback.ProductFallBack;
import com.atguigu.result.RetVal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@FeignClient(value = "shop-cart")
public interface CartFeignClient {

    //加入购物车
    @PostMapping("/cart/addToCart/{skuId}/{skuNum}")
    public RetVal addToCart(@PathVariable Long skuId, @PathVariable Long skuNum);

    //查询选中的商品信息
    @GetMapping("/cart/getSelectedProduct/{userId}")
    public List<CartInfo> getSelectedProduct(@PathVariable String userId);

    //6.从数据库中查询出最新的购物车信息到redis中
    @GetMapping("queryFromDbToRedis/{userId}")
    public List<CartInfo> queryFromDbToRedis(@PathVariable String userId);

}
