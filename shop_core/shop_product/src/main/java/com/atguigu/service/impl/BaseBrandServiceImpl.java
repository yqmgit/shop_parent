package com.atguigu.service.impl;

import com.atguigu.entity.BaseBrand;
import com.atguigu.exception.SleepUtils;
import com.atguigu.mapper.BaseBrandMapper;
import com.atguigu.service.BaseBrandService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.netty.util.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 品牌表 服务实现类
 * </p>
 *
 * @author zhangqiang
 * @since 2021-10-29
 */
@Service
public class BaseBrandServiceImpl extends ServiceImpl<BaseBrandMapper, BaseBrand> implements BaseBrandService {

    @Autowired
    private RedisTemplate redisTemplate;

    //@Override
    public void setNum0() {
    doBusiness();
    }
    //加锁
    //@Override
    public synchronized void setNum1() {
        doBusiness();
    }

    private void doBusiness(){
        String value = (String) redisTemplate.opsForValue().get("num");
        if (StringUtils.isEmpty(value)) {
            redisTemplate.opsForValue().set("num", "1");
        } else {
            int num = Integer.parseInt(value);
            //加一
            redisTemplate.opsForValue().set("num", String.valueOf(++num));
        }
    }

    //分布式锁方案一:如果doBusiness()出现异常，锁会一直占用，无法释放
    //@Override
    public  void setNum2() {
        //利用redis的setnx命令/只有在key的值不存在时设置key的值
        boolean acquireLock = redisTemplate.opsForValue().setIfAbsent("lock", "ok");
        if (acquireLock){
            doBusiness();
            //业务完成后删除
            redisTemplate.delete("lock");
        }else{
            SleepUtils.sleepMillis(50);
            //自旋
            setNum();
        }
    }

    //分布式锁方案二
    //@Override
    public  void setNum3() {
        //利用redis的setnx命令/只有在key的值不存在时设置key的值
        boolean acquireLock = redisTemplate.opsForValue().setIfAbsent("lock", "ok",3, TimeUnit.SECONDS);
        if (acquireLock){
            doBusiness();
            //业务完成后删除
            redisTemplate.delete("lock");
        }else{
            SleepUtils.sleepMillis(50);
            //自旋
            setNum();
        }
    }

    //分布式锁方案三
    //@Override
    public  void setNum4() {
        String uuid = UUID.randomUUID().toString();
        //利用redis的setnx命令/只有在key的值不存在时设置key的值
        boolean acquireLock = redisTemplate.opsForValue().setIfAbsent("lock", uuid,3, TimeUnit.SECONDS);
        if (acquireLock){
            doBusiness();
            //删除之前判断是否为自己的锁
            String lock = (String) redisTemplate.opsForValue().get("lock");
            if (uuid.equals(lock)) {
                //业务完成后删除
                redisTemplate.delete("lock");
            }
        }else{
            SleepUtils.sleepMillis(50);
            //自旋
            setNum();
        }
    }

    //分布式锁方案四
    @Override
    public  void setNum() {
        String uuid = UUID.randomUUID().toString();
        //利用redis的setnx命令/只有在key的值不存在时设置key的值
        boolean acquireLock = redisTemplate.opsForValue().setIfAbsent("lock", uuid,3, TimeUnit.SECONDS);
        if (acquireLock){
            doBusiness();
            //定义一个脚本：把判断和删除两个操作原子性
            String luaScript="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            //把脚本封装到redisScript对象中
            redisScript.setScriptText(luaScript);
            //设置执行脚本之后返回上面类型
            redisScript.setResultType(Long.class);
            //准备执行脚本
            redisTemplate.execute(redisScript, Arrays.asList("lock"),uuid);
        }else{
            SleepUtils.sleepMillis(50);
            //自旋
            setNum();
        }
    }
}