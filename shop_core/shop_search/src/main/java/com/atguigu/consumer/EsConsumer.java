package com.atguigu.consumer;

import com.atguigu.constant.MqConst;
import com.atguigu.service.SearchService;
import com.rabbitmq.client.Channel;
import io.lettuce.core.Value;
import io.lettuce.core.dynamic.annotation.Key;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class EsConsumer {
    @Autowired
    SearchService searchService;
    //接收上架消息
    @RabbitListener(bindings = @QueueBinding(
            value =@Queue(value = MqConst.ON_SALE_QUEUE,durable = "false"),
            exchange = @Exchange(value = MqConst.ON_OFF_SALE_EXCHANGE,durable = "false"),
            key = {MqConst.ON_SALE_ROUTING_KEY}))
    public void onSale(Long skuId, Channel channel, Message message) throws IOException {
       if (skuId!=null){
           searchService.onSale(skuId);
       }
        /**
         * 手动签收
         * basicAck(long deliveryTag, boolean multiple)
         * deliveryTag表示签收哪个消息
         * multiple 是应答多个消息，true多个，false只应答一个
         */
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    //接收下架的消息
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.OFF_SALE_QUEUE,durable = "false"),
                                             exchange = @Exchange(value = MqConst.ON_OFF_SALE_EXCHANGE,durable = "false"),
                                             key = {MqConst.OFF_SALE_ROUTING_KEY}))
    public void offSale(Long skuId,Message message,Channel channel) throws IOException {
        if (skuId!=null){
            searchService.offSale(skuId);
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
