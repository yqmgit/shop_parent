package com.atguigu.client;

import com.atguigu.entity.UserAddress;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(value = "shop-user")
public interface UserFeignClient {

    //根据用户id查询用户地址信息
    @GetMapping("/user/getUserAddressById/{userId}")
    public List<UserAddress> getUserAddressById(@PathVariable String userId);
}
