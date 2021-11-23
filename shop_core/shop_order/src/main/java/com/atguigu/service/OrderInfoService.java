package com.atguigu.service;

import com.atguigu.entity.OrderInfo;
import com.atguigu.enums.ProcessStatus;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 订单表 订单表 服务类
 * </p>
 *
 * @author zhangqiang
 * @since 2021-11-10
 */
public interface OrderInfoService extends IService<OrderInfo> {

    String geterateTradeNo(String userId);

    Long saveOrderAndDetailInfo(OrderInfo orderInfo);

    boolean equalsTradeNo(String tradeNo, String userId);

    void deleteRedisTrade(String userId);

    List<String> checkStockAndPrice(OrderInfo orderInfo, String userId);

    OrderInfo getOrderInfo(Long orderId);

    void updateOrderStatus(OrderInfo orderInfo, ProcessStatus processStatus);

    void sendMsgToWare(OrderInfo orderInfo);
}
