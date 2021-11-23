package com.atguigu.client;

import com.atguigu.entity.OrderInfo;
import com.atguigu.result.RetVal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "shop-order")
public interface OrderFeignClient {
    @GetMapping("order/confirm")
    public RetVal confirm();

    //根据orderId查询订单信息
    @GetMapping("order/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable Long orderId);
}
