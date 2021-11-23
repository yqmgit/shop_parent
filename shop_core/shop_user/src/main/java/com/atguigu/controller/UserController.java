package com.atguigu.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.UserInfo;
import com.atguigu.result.RetVal;
import com.atguigu.service.UserInfoService;
import com.atguigu.util.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserInfoService userInfoService;
    @Autowired
    RedisTemplate redisTemplate;

    @PostMapping("/login")
    public RetVal login(@RequestBody UserInfo userInfo, HttpServletRequest request){
        UserInfo dbUserInfo = userInfoService.queryUserFromDb(userInfo);
        if (dbUserInfo!=null){
            //生成token返回页面cookie
            Map<String, Object> retValMap = new HashMap<>();
            String token = UUID.randomUUID().toString();
            retValMap.put("token",token);

            String nickName = dbUserInfo.getNickName();
            retValMap.put("nickName",nickName);

            String userKey = RedisConst.USER_LOGIN_KEY_PREFIX+token;
            JSONObject loginInfo = new JSONObject();
            loginInfo.put("userId",dbUserInfo.getId());
            loginInfo.put("loginIp", IpUtil.getIpAddress(request));
            //将信息存入redis
            redisTemplate.opsForValue().set(userKey,loginInfo.toString(),RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);
            return RetVal.ok(retValMap);
        }else {
           return RetVal.fail().message("登录失败");
        }
    }

    @GetMapping("/logout")
    public RetVal logout(HttpServletRequest request){
        String token = request.getHeader("token");
        String userKey = RedisConst.USER_LOGIN_KEY_PREFIX+token;
        redisTemplate.delete(userKey);
        return RetVal.ok();
    }
}
