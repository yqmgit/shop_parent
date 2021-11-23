package com.atguigu.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.client.CartFeignClient;
import com.atguigu.client.ProductFeignClient;
import com.atguigu.constant.MqConst;
import com.atguigu.entity.OrderDetail;
import com.atguigu.entity.OrderInfo;
import com.atguigu.enums.OrderStatus;
import com.atguigu.enums.ProcessStatus;
import com.atguigu.executor.MyExecutor;
import com.atguigu.mapper.OrderInfoMapper;
import com.atguigu.service.OrderDetailService;
import com.atguigu.service.OrderInfoService;
import com.atguigu.util.HttpClientUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * 订单表 订单表 服务实现类
 * </p>
 *
 * @author zhangqiang
 * @since 2021-11-10
 */
@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    OrderDetailService orderDetailService;
    @Autowired
    ProductFeignClient productFeignClient;
    @Autowired
    CartFeignClient cartFeignClient;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Value("${cancel.order.delay}")
    private Integer cancelOrderDelay;

    @Transactional
    @Override
    public String geterateTradeNo(String userId) {
        //生成流水号
        String tradeNo = UUID.randomUUID().toString();
        //放入redis
        String tradeNoKey = "user:" + userId + ":tradeNo";
        redisTemplate.opsForValue().set(tradeNoKey,tradeNo);
        return tradeNo;
    }

    @Override
    public Long saveOrderAndDetailInfo(OrderInfo orderInfo) {
        //保存订单基本信息
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        //对外订单号 //out_trade_no   //System.currentTimeMillis()获取系统当前时间(毫秒)//Random().nextInt(n) 0～n之间生成随机数
        String outTradeNo = "atguigu"+System.currentTimeMillis()+"" +new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);

        orderInfo.setTradeBody("我的购物商品");
        orderInfo.setCreateTime(new Date());

        //订单支付过期时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE,30);
        orderInfo.setExpireTime(calendar.getTime());

        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());
        baseMapper.insert(orderInfo);
        //保存订单明细
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
        }
        orderDetailService.saveBatch(orderDetailList);

        //超时取消订单
        rabbitTemplate.convertAndSend(
                MqConst.CANCEL_ORDER_EXCHANGE,
                MqConst.CANCEL_ORDER_ROUTE_KEY,
                orderInfo.getId(),
                correlationData -> {
                    correlationData.getMessageProperties().setDelay(cancelOrderDelay);
                    return correlationData;
                }
        );
        return orderInfo.getId();
    }

    //
    @Override
    public boolean equalsTradeNo(String tradeNo, String userId) {
        String tradeNoKey = "user:" + userId + ":tradeNo";
        String redisTrade = (String) redisTemplate.opsForValue().get(tradeNoKey);
        return redisTrade.equals(tradeNo);
    }
//删除redis中的流水号
    @Override
    public void deleteRedisTrade(String userId) {
        String tradeNoKey = "user:" + userId + ":tradeNo";
        redisTemplate.delete(tradeNoKey);
    }

    //优化效率
    //验证库存与价格
    @Override
    public List<String> checkStockAndPrice(OrderInfo orderInfo, String userId) {
        List<String> waringInfoLIst = new ArrayList<>();

        ArrayList<CompletableFuture> completableFutureArrayList = new ArrayList<>();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (!CollectionUtils.isEmpty(orderDetailList)){
            //查看每个商品的情况,
            for (OrderDetail orderDetail : orderDetailList) {
                Long skuId = orderDetail.getSkuId();
                String skuNum = orderDetail.getSkuNum();
                CompletableFuture<Void> checkStockFuture = CompletableFuture.runAsync(() -> {
                    String result = HttpClientUtil.doGet("http://localhost:8100/hasStock?skuId=" + skuId + "&num=" + skuNum);
                    if ("0".equals(result)) {
                        waringInfoLIst.add(orderDetail.getSkuNum() + "库存不足");
                    }
                }, MyExecutor.getInstance());
                completableFutureArrayList.add(checkStockFuture);

                CompletableFuture<Void> checkPriceFuture = CompletableFuture.runAsync(() -> {
                    //比较价格
                    BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
                    if (orderDetail.getOrderPrice().compareTo(skuPrice) != 0) {
                        waringInfoLIst.add(orderDetail.getOrderPrice() + "价格有变化");
                        //更新最新的数据到redis中
                        cartFeignClient.queryFromDbToRedis(userId);
                    }
                }, MyExecutor.getInstance());
                completableFutureArrayList.add(checkPriceFuture);
            }
        }
        //当上面两个异步全部完成后才返回
        CompletableFuture[] completableFutureArray = new CompletableFuture[completableFutureArrayList.size()];
        CompletableFuture.allOf(completableFutureArrayList.toArray(completableFutureArray)).join();
        return waringInfoLIst;
    }

    //根据orderId查询订单信息
    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        OrderInfo orderInfo = baseMapper.selectById(orderId);
        if (orderId!=null){
            QueryWrapper<OrderDetail> wrapper = new QueryWrapper<>();
            wrapper.eq("order_id",orderId);
            List<OrderDetail> orderDetailList = orderDetailService.list(wrapper);
            orderInfo.setOrderDetailList(orderDetailList);
        }
        return orderInfo;
    }

    @Override
    public void updateOrderStatus(OrderInfo orderInfo, ProcessStatus processStatus) {
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        orderInfo.setProcessStatus(processStatus.name());
        baseMapper.updateById(orderInfo);
    }

    @Override
    public void sendMsgToWare(OrderInfo orderInfo) {
        //更改状态（已通知仓库）
        updateOrderStatus(orderInfo,ProcessStatus.NOTIFIED_WARE);
        //库存系统传入参数封装
        String jsonData = assembleWareData(orderInfo);
        //发送消息给仓库系统
        rabbitTemplate.convertAndSend(MqConst.DECREASE_STOCK_EXCHANGE,MqConst.DECREASE_STOCK_ROUTE_KEY,jsonData);
    }

    private String assembleWareData(OrderInfo orderInfo) {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("orderId",orderInfo.getId());
        dataMap.put("consignee",orderInfo.getConsignee());
        dataMap.put("consigneeTel",orderInfo.getConsigneeTel());
        dataMap.put("orderComment",orderInfo.getOrderComment());
        dataMap.put("orderBody",orderInfo.getTradeBody());
        dataMap.put("deliveryAddress",orderInfo.getDeliveryAddress());
        dataMap.put("paymentWay",2);

        List<Map> orderDetailMapList = new ArrayList<>();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            Map<String, Object> orderDetailMap = new HashMap<>();
            orderDetailMap.put("skuId",orderDetail.getSkuId());
            orderDetailMap.put("skuNum",orderDetail.getSkuNum());
            orderDetailMap.put("skuName",orderDetail.getSkuName());
            orderDetailMapList.add(orderDetailMap);
        }
        dataMap.put("details",orderDetailMapList);
        String jsonData = JSON.toJSONString(dataMap);
        return jsonData;
    }
/*
    //验证库存与价格（优化前）
    @Override
    public List<String> checkStockAndPrice(OrderInfo orderInfo, String userId) {
        List<String> waringInfoLIst = new ArrayList<>();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (!CollectionUtils.isEmpty(orderDetailList)){
            //查看每个商品的情况,
            for (OrderDetail orderDetail : orderDetailList) {
                Long skuId = orderDetail.getSkuId();
                String skuNum = orderDetail.getSkuNum();
                String result = HttpClientUtil.doGet("http://localhost:8100/hasStock?skuId=" + skuId + "&num=" + skuNum);
                if ("0".equals(result)){
                    waringInfoLIst.add(orderDetail.getSkuNum()+"库存不足") ;
                }
                //比较价格
                BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
                if (orderDetail.getOrderPrice().compareTo(skuPrice)!=0){
                    waringInfoLIst.add(orderDetail.getOrderPrice()+"价格有变化");
                    //更新最新的数据到redis中
                    cartFeignClient.queryFromDbToRedis(userId);
                }
            }
        }
        return waringInfoLIst;
    }
*/
}
