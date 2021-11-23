package com.atguigu.consumer;

import com.atguigu.constant.MqConst;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.SeckillProduct;
import com.atguigu.service.SeckillProductService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import com.atguigu.utils.DateUtil;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

@Component
public class SeckillConsumer {

    @Autowired
    private SeckillProductService seckillProductService;
    @Autowired
    private RedisTemplate redisTemplate;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.SCAN_SECKILL_QUEUE,durable = "false"),
            exchange = @Exchange(value = MqConst.SCAN_SECKILL_EXCHANGE,durable = "false"),
            key = (MqConst.SCAN_SECKILL_ROUTE_KEY)))
    public void scanSeckillProductToRedis(Message message, Channel channel){
        //扫描秒杀商品
        QueryWrapper<SeckillProduct> wrapper = new QueryWrapper<>();
        //状态是可以秒杀
        wrapper.eq("status",1);
        //库存数量大于0
        wrapper.gt("stock_count",0);
        //日期是当天时间 SELECT * FROM seckill_product WHERE DATE_FORMAT(start_time,'%Y-%m-%d')='2021-11-22'
        wrapper.eq("DATE_FORMAT(start_time,'%Y-%m-%d')", DateUtil.formatDate(new Date()));
        List<SeckillProduct> seckillProductList = seckillProductService.list(wrapper);
        //扫描商品到redis
        if (!CollectionUtils.isEmpty(seckillProductList)){
            seckillProductList.stream().forEach(seckillProduct -> {
                redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).put(seckillProduct.getId().toString(),seckillProduct);
                for (int i = 0; i <seckillProduct.getNum() ; i++) {
                    redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX+seckillProduct.getSkuId())
                            .leftPush(seckillProduct.getSkuId().toString());
                }
            });
        }
    }
}
