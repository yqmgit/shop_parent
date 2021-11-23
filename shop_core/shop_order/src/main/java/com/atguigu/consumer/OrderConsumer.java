package com.atguigu.consumer;

import com.alibaba.fastjson.JSON;
import com.atguigu.constant.MqConst;
import com.atguigu.entity.OrderInfo;
import com.atguigu.enums.OrderStatus;
import com.atguigu.enums.ProcessStatus;
import com.atguigu.service.OrderInfoService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;


@Component
public class OrderConsumer {
    @Autowired
    private OrderInfoService orderInfoService;

    /**
     * 1.取消订单的监视器
     */
    @RabbitListener(queues = MqConst.CANCEL_ORDER_QUEUE)
    public void canCelOrder(Long orderId) {
        if (orderId != null) {
            //根据订单id查询订单信息
            OrderInfo orderInfo = orderInfoService.getById(orderId);
            //修改订单状态为关闭状态
            orderInfoService.updateOrderStatus(orderInfo, ProcessStatus.CLOSED);
        }
    }

    //支付成功后修改订单信息
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.PAY_ORDER_QUEUE, durable = "false"),
            exchange = @Exchange(value = MqConst.PAY_ORDER_EXCHANGE, durable = "false"),
            key = (MqConst.PAY_ORDER_ROUTE_KEY)))
    public void updateOrderInfo(Long orderId, Channel channel, Message message) throws Exception {
        //查询订单对象
        OrderInfo orderInfo = orderInfoService.getOrderInfo(orderId);
        if (orderInfo!=null&&orderInfo.getOrderStatus().equals(OrderStatus.UNPAID.name())){
            orderInfoService.updateOrderStatus(orderInfo,ProcessStatus.PAID);
            //通知库存系统减库存
            orderInfoService.sendMsgToWare(orderInfo);
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    //减库存成功之后修改订单状态
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.SUCCESS_DECREASE_STOCK_QUEUE,durable = "false"),
            exchange = @Exchange(value = MqConst.SUCCESS_DECREASE_STOCK_EXCHANGE,durable = "false"),
            key = (MqConst.SUCCESS_DECREASE_STOCK_ROUTE_KEY)))
    public void updateOrderStatus(String dataMap,Message message,Channel channel) throws IOException {
        //从传入的数据中解析出订单id和状态
        Map<String,String> parseObject = JSON.parseObject(dataMap, Map.class);
        String orderId =  parseObject.get("orderId");
        String status = parseObject.get("status");
        OrderInfo orderInfo = orderInfoService.getOrderInfo(Long.parseLong(orderId));
        //减库存成功就把订单状态改为未发货
        if ("DEDUCTED".equals(status)){
            orderInfoService.updateOrderStatus(orderInfo,ProcessStatus.WAITING_DELEVER);
        }else {
            orderInfoService.updateOrderStatus(orderInfo,ProcessStatus.STOCK_EXCEPTION);
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}

