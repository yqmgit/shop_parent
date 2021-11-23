package com.atguigu.controller;


import com.atguigu.entity.CartInfo;
import com.atguigu.result.RetVal;
import com.atguigu.service.CartInfoService;
import com.atguigu.util.AuthContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.aspectj.apache.bcel.generic.RET;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.security.PublicKey;
import java.util.List;

/**
 * <p>
 * 购物车表 用户登录系统时更新冗余 前端控制器
 * </p>
 *
 * @author zhangqiang
 * @since 2021-11-10
 */
@RestController
@RequestMapping("/cart")
public class CartInfoController {

    @Autowired
    private CartInfoService cartInfoService;

    @PostMapping("/addToCart/{skuId}/{skuNum}")
    public RetVal addToCart(@PathVariable Long skuId, @PathVariable Integer skuNum, HttpServletRequest request){
        //request.getHeader("userId")
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)){
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartInfoService.addToCart(userId,skuId,skuNum);

        return RetVal.ok();
    }

    //获得购物车列表
    @GetMapping("getCartList")
    public RetVal getCartList(HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        String userTempId = AuthContextHolder.getUserTempId(request);
        List<CartInfo> cartInfoList = cartInfoService.getCartList(userId,userTempId);
        return RetVal.ok(cartInfoList);
    }
//删除购物车
    @DeleteMapping("/deleteCart/{skuId}")
    public RetVal deleteCart(@PathVariable Long skuId){

        cartInfoService.deleteCart(skuId);
        return RetVal.ok();
    }

    //勾选
    @GetMapping("/checkCart/{skuId}/{isChecked}")
    public RetVal checkCart(@PathVariable Long skuId,@PathVariable Integer isChecked,HttpServletRequest request){
        cartInfoService.checkCart(skuId,isChecked,request);
        return RetVal.ok();
    }

    //查询选中的商品信息
    @GetMapping("getSelectedProduct/{userId}")
    public List<CartInfo> getSelectedProduct(@PathVariable String userId){
        return cartInfoService.getSelectedProduct(userId);
    }


    //6.从数据库中查询出最新的购物车信息到redis中
    @GetMapping("queryFromDbToRedis/{userId}")
    public List<CartInfo> queryFromDbToRedis(@PathVariable String userId){
        return cartInfoService.queryFromDbToRedis(userId);
    }
}

