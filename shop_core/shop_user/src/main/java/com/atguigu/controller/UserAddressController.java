package com.atguigu.controller;


import com.atguigu.entity.UserAddress;
import com.atguigu.service.UserAddressService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 用户地址表 前端控制器
 * </p>
 *
 * @author 小叶子
 * @since 2021-11-15
 */
@RestController
@RequestMapping("/user")
public class UserAddressController {

    @Autowired
    UserAddressService userAddressService;

    //根据用户id获取用户地址
    @GetMapping("/getUserAddressById/{userId}")
    public List<UserAddress> getUserAddressById(@PathVariable String userId){
        QueryWrapper<UserAddress> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",userId);
        return userAddressService.list(wrapper);
}
}

