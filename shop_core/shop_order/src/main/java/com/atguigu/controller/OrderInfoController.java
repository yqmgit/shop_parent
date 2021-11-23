package com.atguigu.controller;


import com.atguigu.client.CartFeignClient;
import com.atguigu.client.UserFeignClient;
import com.atguigu.constant.MqConst;
import com.atguigu.entity.CartInfo;
import com.atguigu.entity.OrderDetail;
import com.atguigu.entity.OrderInfo;
import com.atguigu.entity.UserAddress;
import com.atguigu.result.RetVal;
import com.atguigu.service.OrderInfoService;
import com.atguigu.util.AuthContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;

/**
 * <p>
 * 订单表 订单表 前端控制器
 * </p>
 *
 * @author zhangqiang
 * @since 2021-11-10
 */
@RestController
@RequestMapping("/order")
public class OrderInfoController {

    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    CartFeignClient cartFeignClient;
    @Autowired
    OrderInfoService orderInfoService;

    //订单确认数据接口
    @GetMapping("/confirm")
    public RetVal confirm(HttpServletRequest request){
        //收货人地址信息
        String userId =AuthContextHolder.getUserId(request);
        List<UserAddress> userAddressList = userFeignClient.getUserAddressById(userId);
        //送货清单
        List<CartInfo> selectedProductList = cartFeignClient.getSelectedProduct(userId);
        //总金额
        BigDecimal totalMoney = new BigDecimal(0);
        //总件数
        int totalNum =0;

        List<OrderDetail> orderDetailList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(selectedProductList)){
            for (CartInfo cartInfo : selectedProductList) {
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setOrderPrice(cartInfo.getCartPrice());
                orderDetail.setSkuNum(cartInfo.getSkuNum()+"");
                orderDetailList.add(orderDetail);
                //总金额
                totalMoney=totalMoney.add(cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum())));
                totalNum+=cartInfo.getSkuNum();
            }
        }
        //封装页面所需的数据
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("userAddressList",userAddressList);
        retMap.put("detailArrayList",orderDetailList);
        retMap.put("totalMoney",totalMoney);
        retMap.put("totalNum",totalNum);
        //生成流水号
        String tradeNo = orderInfoService.geterateTradeNo(userId);
        retMap.put("tradeNo",tradeNo);
        return RetVal.ok(retMap);
    }


    //提交订单
    @PostMapping("/submitOrder")
    public RetVal submitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        //防止重复提交订单，验证流水号
        String tradeNo = request.getParameter("tradeNo");
        //对比redis中的流水号，不同提示重复提交
        boolean b = orderInfoService.equalsTradeNo(tradeNo, userId);
        if (!b){
            return RetVal.fail().message("不能重复提交");
        }
        //删除redis
        orderInfoService.deleteRedisTrade(userId);

        //验证库存与价格
        List<String> waringInfoList = orderInfoService.checkStockAndPrice(orderInfo, userId);
        if (waringInfoList.size()>0){
            return RetVal.fail().message(StringUtils.join(waringInfoList,"，"));
        }
        orderInfoService.saveOrderAndDetailInfo(orderInfo);

        return RetVal.ok(orderInfo.getId());
    }
    //根据orderId查询订单信息
    @GetMapping("getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable Long orderId){
        return orderInfoService.getOrderInfo(orderId);
    }
}

