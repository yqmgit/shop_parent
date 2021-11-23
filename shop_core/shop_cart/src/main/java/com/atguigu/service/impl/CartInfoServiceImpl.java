package com.atguigu.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.client.ProductFeignClient;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.CartInfo;
import com.atguigu.entity.SkuInfo;
import com.atguigu.mapper.CartInfoMapper;
import com.atguigu.service.AsyncCartInfoService;
import com.atguigu.service.CartInfoService;
import com.atguigu.util.AuthContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 购物车表 用户登录系统时更新冗余 服务实现类
 * </p>
 *
 * @author zhangqiang
 * @since 2021-11-10
 */
@Service
public class CartInfoServiceImpl extends ServiceImpl<CartInfoMapper, CartInfo> implements CartInfoService {

    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private AsyncCartInfoService asyncCartInfoService;

    @Override
    public void addToCart(String userId, Long skuId, Integer skuNum) {
        //1.先查询数据库有没有该商品信息
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",userId)
                .eq("sku_id",skuId);
        CartInfo existCartInfo = baseMapper.selectOne(wrapper);
        if (existCartInfo!=null){
            //有的话和传递过来的数量相加
            existCartInfo.setSkuNum(existCartInfo.getSkuNum()+skuNum);
            //更新实施价格
            existCartInfo.setRealTimePrice(productFeignClient.getSkuPrice(skuId));
            //更新数据库
            //baseMapper.updateById(existCartInfo);
            asyncCartInfoService.updateCart(existCartInfo);

        }else{
            //加入数据库/redis
            CartInfo cartInfo = new CartInfo();
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            cartInfo.setUserId(userId);
            cartInfo.setSkuId(skuId);
            cartInfo.setCartPrice(productFeignClient.getSkuPrice(skuId));
            cartInfo.setSkuNum(skuNum);
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuName(skuInfo.getSkuName());
            //默认勾选
            cartInfo.setIsChecked(1);
            //baseMapper.insert(cartInfo);
            asyncCartInfoService.addCart(cartInfo);

        }
        //存储到redis
        String userCartKey= getUserCartKey(userId);
        redisTemplate.boundHashOps(userCartKey).put(skuId.toString(),existCartInfo);
        //设置过期时间
        setCartKeyExpire(userCartKey);
    }

    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        //用户未登录
        if (userTempId!=null){
            cartInfoList = getUserCartList(userTempId);
        }
        //用户已登录
        if (!StringUtils.isEmpty(userId)){
                //查询未登录的信息
               List<CartInfo> noLoginCartInfoList = getUserCartList(userTempId);
                if(!CollectionUtils.isEmpty(noLoginCartInfoList)){
                    //合并未登录和已登录购物车信息
                    cartInfoList=mergeCartInfoList(noLoginCartInfoList,userId);
                    //合并之后删除未登录信息
                    deleteNoLoginCartInfoList(userTempId);
                }else{
                    cartInfoList=queryFromDbToRedis(userId);
                }
        }

        return cartInfoList;
    }

    @Override
    public void deleteCart(Long skuId) {
        //删除数据库中的数据
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("sku_id",skuId);
        CartInfo cartInfo = baseMapper.selectOne(wrapper);
        baseMapper.deleteById(cartInfo.getId());
        //删除redis中的数据
        String deleteKey = getUserCartKey(cartInfo.getUserId());
        BoundHashOperations boundHashOps = redisTemplate.boundHashOps(deleteKey);
        if (boundHashOps.hasKey(skuId.toString())){
            //删除
            boundHashOps.delete(skuId.toString());
        }
    }

    //勾选商品修改数据库和redis中的isChecked属性
    @Override
    public void checkCart(Long skuId, Integer isChecked, HttpServletRequest request) {
        //拿到用户ID
        String userId = AuthContextHolder.getUserId(request);
        //修改redis
        String redisCartKey = getUserCartKey(userId);
        BoundHashOperations boundHashOps = redisTemplate.boundHashOps(redisCartKey);
        if (boundHashOps.hasKey(skuId.toString())){
            CartInfo redisCartInfo = (CartInfo) boundHashOps.get(skuId.toString());
            redisCartInfo.setIsChecked(isChecked);
            //更新到redis中
            boundHashOps.put(skuId.toString(),redisCartInfo);
            //设置过期时间
            setCartKeyExpire(redisCartKey);
        }

        //修改数据库
        asyncCartInfoService.checkCart(userId,skuId,isChecked);
    }

    //合并之后删除未登录信息
    private void deleteNoLoginCartInfoList(String userTempId) {
        //删除数据库里的信息
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",userTempId);
        CartInfo cartTempInfo = baseMapper.selectOne(wrapper);
        //baseMapper.deleteById(cartTempInfo.getId());
        Long userId = cartTempInfo.getId();
        asyncCartInfoService.deleteByUserId(userId);

        //删除redis中的信息
        String userCartKey = getUserCartKey(userTempId);
        Boolean flag = redisTemplate.hasKey(userCartKey);
        if(flag){
            redisTemplate.delete(userCartKey);
        }
    }

    //合并未登录和已登录购物车信息
    private List<CartInfo> mergeCartInfoList(List<CartInfo> noLoginCartInfoList, String userId) {
        //取出登录的用户中购物车的信息
        List<CartInfo> loginUserCartList = getUserCartList(userId);
        //把其中一个转换成map结构
        Map<Long, CartInfo> longCartInfoMap = loginUserCartList.stream().collect(Collectors.toMap(CartInfo::getSkuId, cartInfo -> cartInfo));
        for (CartInfo noLoginCartInfo : noLoginCartInfoList) {
            Long skuId = noLoginCartInfo.getSkuId();
            //判断登录map中是否含有未登录信息的skuId
            if (longCartInfoMap.containsKey(skuId)){
                //有的话数量相加
                CartInfo loginCartInfo = longCartInfoMap.get(skuId);
                loginCartInfo.setSkuNum(noLoginCartInfo.getSkuNum()+loginCartInfo.getSkuNum());
                //未登录时商品是勾选状态，登录后也要保持
                if (noLoginCartInfo.getIsChecked()==1){
                    loginCartInfo.setIsChecked(1);
                }
                //baseMapper.updateById(loginCartInfo);
                asyncCartInfoService.updateByTempId(noLoginCartInfo);
            }else {
                //把临时id改成登录后的id
                noLoginCartInfo.setUserId(userId);
                //baseMapper.updateById(noLoginCartInfo);
                asyncCartInfoService.updateByTempId(noLoginCartInfo);
            }
        }
        //合并之后更新缓存
        List<CartInfo> cartInfoList = queryFromDbToRedis(userId);
        return cartInfoList;
    }

    private List<CartInfo> getUserCartList(String oneOfUserId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        if(StringUtils.isEmpty(oneOfUserId)){
            return cartInfoList;
        }
        //从数据库中查询数据到数据库
        cartInfoList=queryFromDbToRedis(oneOfUserId);
        return cartInfoList;
    }

    //更新缓存信息
    @Override
    public List<CartInfo> queryFromDbToRedis(String userTempId) {
////        //先查缓存
//        String cartInfoKey = RedisConst.USER_KEY_PREFIX+userTempId+RedisConst.USER_CART_KEY_SUFFIX;
//        String cartInfoList = (String) redisTemplate.opsForValue().get(cartInfoKey);
//        if (!StringUtils.isEmpty(cartInfoList)){
//            List<CartInfo> myCartInfoList = JSON.parseArray(cartInfoList,CartInfo.class);
//        }

        //根据临时id查询购物车信息
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",userTempId);
        List<CartInfo> dbCartInfoList = baseMapper.selectList(wrapper);
        //如果数据库中不存在用户添加的购物车信息
        if(CollectionUtils.isEmpty(dbCartInfoList)){
            return dbCartInfoList;
        }
        String userCartKey = getUserCartKey(userTempId);
        HashMap<String, CartInfo> cartInfoMap = new HashMap<>();
        for (CartInfo cartInfo : dbCartInfoList) {
            //方式一
            //redisTemplate.opsForHash().put(userCartKey,cartInfo.getSkuId().toString(),cartInfo);
            //更新实时价格
            cartInfo.setRealTimePrice(productFeignClient.getSkuPrice(cartInfo.getSkuId()));
            cartInfoMap.put(cartInfo.getSkuId().toString(),cartInfo);
        }
        redisTemplate.opsForHash().putAll(userCartKey,cartInfoMap);
        //设置过期时间
        setCartKeyExpire(userCartKey);
        return dbCartInfoList;
    }

    //通过userId查询选中的商品信息
    @Override
    public List<CartInfo> getSelectedProduct(String userId) {
        //从redis中查
        List<CartInfo> SelectedCartInfoList = new ArrayList<>();
        String userCartKey = getUserCartKey(userId);
        List<CartInfo> cartInfoList = redisTemplate.opsForHash().values(userCartKey);
        if (!CollectionUtils.isEmpty(cartInfoList)){
            for (CartInfo cartInfo : cartInfoList) {
                if (cartInfo.getIsChecked()==1){
                    SelectedCartInfoList.add(cartInfo);
                }
            }
        }
        return SelectedCartInfoList;
    }


    //设置过期时间
    private void setCartKeyExpire(String userCartKey) {
        redisTemplate.expire(userCartKey,RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }

    //redis中的key
    private String getUserCartKey(String userId) {
        return RedisConst.USER_KEY_PREFIX+userId+RedisConst.USER_CART_KEY_SUFFIX;
    }
}
