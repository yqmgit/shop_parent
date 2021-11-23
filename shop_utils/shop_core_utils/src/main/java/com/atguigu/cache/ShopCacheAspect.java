package com.atguigu.cache;

import com.atguigu.constant.RedisConst;
import com.atguigu.exception.SleepUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Component
//表明该类是一个切面类
@Aspect
public class ShopCacheAspect {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    //只要方法上有ShopCache的注解，就执行下面的切面逻辑
    @Around("@annotation(com.atguigu.cache.ShopCache)")
    public Object cacheAroundAdvice(ProceedingJoinPoint target){
        //target代表目标类

        //
        MethodSignature signature = (MethodSignature) target.getSignature();
        Method targetMethod = signature.getMethod();
        ShopCache shopCache = targetMethod.getAnnotation(ShopCache.class);
        String prefix = shopCache.prefix();
        Object[] targetParams = target.getArgs();
        //拼接
        String skuKey = prefix + Arrays.asList(targetParams).toString();

        Object retVal = redisTemplate.opsForValue().get(skuKey);
        if (retVal==null){
            String lockKey = skuKey + RedisConst.SKULOCK_SUFFIX;
            RLock lock = redissonClient.getLock(lockKey);
            try {
                boolean acquireLock = lock.tryLock(RedisConst.WAITTIN_GET_LOCK_TIME, RedisConst.LOCK_EXPIRE_TIME, TimeUnit.SECONDS);
                if (acquireLock) {
                    //b.从数据库中拿skuInfo的信息
                    retVal = target.proceed();
                    //判断skuInfoFromDb是否为空 可能会引起缓存穿透
                    if (retVal == null) {
                        Object emptySkuInfo = new Object();
                        redisTemplate.opsForValue().set(skuKey, emptySkuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                        return emptySkuInfo;
                    }
                    //c.把数据放到缓存中
                    redisTemplate.opsForValue().set(skuKey, retVal, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                    return retVal;
                } else {
                    //设置睡眠时间
                    SleepUtils.sleepMillis(50);
                    //自旋
                    return cacheAroundAdvice(target);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            } finally {
                lock.unlock();
            }
        } else {
            //d.把数据返回给调用方
            return retVal;
        }
        return null;
    }

}
