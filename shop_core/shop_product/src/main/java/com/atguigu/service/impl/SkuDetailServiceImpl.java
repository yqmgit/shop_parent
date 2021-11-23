package com.atguigu.service.impl;

import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuImage;
import com.atguigu.entity.SkuInfo;
import com.atguigu.exception.SleepUtils;
import com.atguigu.mapper.ProductSalePropertyKeyMapper;
import com.atguigu.mapper.SkuSalePropertyValueMapper;
import com.atguigu.service.SkuDetailService;
import com.atguigu.service.SkuImageService;
import com.atguigu.service.SkuInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class SkuDetailServiceImpl implements SkuDetailService {

    @Autowired
    private SkuInfoService skuInfoService;

    @Autowired
    private SkuImageService skuImageService;

    @Autowired
    private ProductSalePropertyKeyMapper salePropertyKeyMapper;

    @Autowired
    SkuSalePropertyValueMapper skuSalePropertyValueMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;


    @Override
    public List<ProductSalePropertyKey> getSpuSalePropertyAndSelected(Long productId, Long skuId) {
        return salePropertyKeyMapper.getSpuSalePropertyAndSelected(productId,skuId);
    }

    @Override
    public Map getSalePropertyAndSkuIdMapping(Long productId) {
        Map<Object,Object> retMap=new HashMap<>();

        List<Map> valueIdMap=skuSalePropertyValueMapper.getSalePropertyAndSkuIdMapping(productId);
        if(!CollectionUtils.isEmpty(valueIdMap)){
            for (Map map : valueIdMap) {
                retMap.put(map.get("sale_property_value_id"),map.get("sku_id"));
            }
        }
        return retMap;
    }
    @Override
    public SkuInfo getSkuInfo(Long skuId) {

        SkuInfo skuInfo = getSkuInfoFromDb(skuId);
        //SkuInfo skuInfo = getSkuInfoFromRedission(skuId);
        //SkuInfo skuInfo = getSkuInfoFromRedis(skuId);
        return skuInfo;
    }

    //利用redission解决线程不安全的问题
    public SkuInfo getSkuInfoFromRedission(Long skuId){
        //从缓存拿skuInfo的信息
        String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
        SkuInfo skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
        if (skuInfo==null){
            String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
            RLock lock = redissonClient.getLock(lockKey);
            try {
            boolean acquireLock = lock.tryLock(RedisConst.WAITTIN_GET_LOCK_TIME, RedisConst.LOCK_EXPIRE_TIME, TimeUnit.SECONDS);
            if (acquireLock){
                //从数据库拿skuInfo
                SkuInfo skuInfoFromDb = getSkuInfoFromDb(skuId);
                if (skuInfoFromDb == null) {
                    SkuInfo emptySkuInfo = new SkuInfo();
                    redisTemplate.opsForValue().set(skuKey, emptySkuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                    return emptySkuInfo;
                }
                //c.把数据放到缓存中
                redisTemplate.opsForValue().set(skuKey, skuInfoFromDb, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                return skuInfoFromDb;
            } else {
                //设置睡眠时间
                SleepUtils.sleepMillis(50);
                //自旋
                return getSkuInfo(skuId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    } else {
        return skuInfo;
        }
        return null;
    }

    //利用redis+lua脚本解决线程不安全的问题
    public SkuInfo getSkuInfoFromRedis(Long skuId) {
        //a.从缓存中拿去skuInfo的信息 sku:24:info
        String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
        SkuInfo skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
        if (skuInfo == null) {
            String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
            String uuid = UUID.randomUUID().toString();
            //利用redis的setnx命令 该键只能存放3秒钟
            boolean acquireLock = redisTemplate.opsForValue().setIfAbsent(lockKey, uuid, 3, TimeUnit.SECONDS);
            if (acquireLock) {
                //b.从数据库中拿skuInfo的信息
                SkuInfo skuInfoFromDb = getSkuInfoFromDb(skuId);
                //判断skuInfoFromDb是否为空 可能会引起缓存穿透
                if (skuInfoFromDb == null) {
                    SkuInfo emptySkuInfo = new SkuInfo();
                    redisTemplate.opsForValue().set(skuKey, emptySkuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                    return emptySkuInfo;
                }
                //c.把数据放到缓存中
                redisTemplate.opsForValue().set(skuKey, skuInfoFromDb, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                //准备释放锁的逻辑
                String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                //把脚本封装到redisScript对象中
                redisScript.setScriptText(luaScript);
                //设置执行脚本之后返回上面类型
                redisScript.setResultType(Long.class);
                //准备执行脚本
                redisTemplate.execute(redisScript, Arrays.asList(lockKey), uuid);
                return skuInfoFromDb;
            } else {
                //设置睡眠时间
                SleepUtils.sleepMillis(50);
                //自旋
                return getSkuInfo(skuId);
            }
        } else {
            return skuInfo;
        }
    }


    private SkuInfo getSkuInfoFromDb(Long skuId){
        SkuInfo skuInfo = skuInfoService.getById(skuId);
        if (skuInfo!=null){
            QueryWrapper<SkuImage> wrapper = new QueryWrapper<>();
            wrapper.eq("sku_id",skuId);
            List<SkuImage> imageList = skuImageService.list(wrapper);
            skuInfo.setSkuImageList(imageList);
        }
        return skuInfo;
    }
}
