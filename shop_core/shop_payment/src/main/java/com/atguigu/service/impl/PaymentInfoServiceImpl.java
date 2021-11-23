package com.atguigu.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.*;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.client.OrderFeignClient;
import com.atguigu.config.AlipayConfig;
import com.atguigu.constant.MqConst;
import com.atguigu.entity.OrderInfo;
import com.atguigu.entity.PaymentInfo;
import com.atguigu.enums.PaymentStatus;
import com.atguigu.enums.PaymentType;
import com.atguigu.enums.ProcessStatus;
import com.atguigu.mapper.PaymentInfoMapper;
import com.atguigu.service.PaymentInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 支付信息表 服务实现类
 * </p>
 *
 * @author zhangqiang
 * @since 2021-11-10
 */
@Service
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {

    @Autowired
    OrderFeignClient orderFeignClient;
    @Autowired
    private AlipayClient alipayClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;


    @Override
    public String createQrCode(Long orderId) throws Exception{
        //1.根据订单id查询订单信息
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        //2.保存支付信息
        savePayInfo(orderInfo);
        //3.调用支付宝返回二维码的工具类
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        //4.设置支付成功之后的异步通知
        request.setNotifyUrl(AlipayConfig.notify_payment_url);
        //5.设置支付成功之后的同步通知
        request.setReturnUrl(AlipayConfig.return_payment_url);
        JSONObject bizContent = new JSONObject();
        //6.商户对外订单号
        bizContent.put("out_trade_no",orderInfo.getOutTradeNo());
        //7.订单总金额
        bizContent.put("total_amount",orderInfo.getTotalMoney());
        //8.订单标题
        bizContent.put("subject", "吃个桃子，好凉凉");
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");

        request.setBizContent(bizContent.toString());
        AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
        if(response.isSuccess()){
            return response.getBody();
            //System.out.println("调用成功");
        } else {
            System.out.println("调用失败");
        }
        return null;
    }

    //根据对外订单号查询订单信息
    @Override
    public PaymentInfo getPaymentStatus(String outTradeNo) {
        QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("out_trade_no",outTradeNo);
        PaymentInfo paymentInfo = baseMapper.selectOne(wrapper);
        return paymentInfo;
    }

    @Override
    public void updatePaymentInfo(Map<String, String> aliparamMap) {
        String outTradeNo = aliparamMap.get("out_trade_no");
        PaymentInfo paymentInfo = getPaymentStatus(outTradeNo);
        paymentInfo.setPaymentStatus(PaymentStatus.PAID.name());
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(aliparamMap.toString());
        String tradeNo = aliparamMap.get("trade_no");
        paymentInfo.setTradeNo(tradeNo);
        baseMapper.updateById(paymentInfo);
        //发消息给shop-order修改订单状态
        rabbitTemplate.convertAndSend(MqConst.PAY_ORDER_EXCHANGE,MqConst.PAY_ORDER_ROUTE_KEY,paymentInfo.getOrderId());
    }

    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo) {
        //判断数据库中是否存在该订单的支付信息
        QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("out_trade_no",outTradeNo);
        wrapper.eq("payment_type", PaymentType.ALIPAY.name());
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean refund(Long orderId) throws Exception {
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);

        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        bizContent.put("refund_amount", orderInfo.getTotalMoney());
        bizContent.put("refund_reason", "坏了");
        request.setBizContent(bizContent.toString());
        AlipayTradeRefundResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            //如果退款成功修改支付订单状态为已关闭
            PaymentInfo paymentInfo = getPaymentInfo(orderInfo.getOutTradeNo());
            paymentInfo.setPaymentStatus(ProcessStatus.CLOSED.name());
            baseMapper.updateById(paymentInfo);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean queryAlipayTrade(Long orderId) throws Exception {
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            return true;
        } else {
            return false;
        }

    }

    @Override
    public boolean closeAlipayTrade(Long orderId) throws Exception {
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        request.setBizContent(bizContent.toString());
        AlipayTradeCloseResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            return true;
        }else{
            return false;
        }

    }

    //保存支付信息
    private void savePayInfo(OrderInfo orderInfo) {
        //判断是否有该订单的支付信息
        QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id",orderInfo.getId());
        wrapper.eq("payment_type",PaymentType.ALIPAY.name());
        Integer count = baseMapper.selectCount(wrapper);
        if (count>0){
            return;
        }
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setOrderId(orderInfo.getId().toString());
        paymentInfo.setPaymentType(PaymentType.ALIPAY.name());
        paymentInfo.setPaymentMoney(orderInfo.getTotalMoney());
        paymentInfo.setPaymentContent(orderInfo.getTradeBody());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setCreateTime(new Date());
        //插入数据库
        baseMapper.insert(paymentInfo);

    }
}
