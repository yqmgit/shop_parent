package com.atguigu.service.impl;

import com.atguigu.entity.CartInfo;
import com.atguigu.mapper.CartInfoMapper;
import com.atguigu.service.AsyncCartInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Async
@Service
public class AsyncCartInfoServiceImpl extends ServiceImpl<CartInfoMapper, CartInfo> implements AsyncCartInfoService {
    @Override
    public void addCart(CartInfo cartInfo) {
        baseMapper.insert(cartInfo);
    }

    @Override
    public void updateCart(CartInfo existCartInfo) {
        baseMapper.updateById(existCartInfo);
    }

    @Override
    public void deleteByUserId(Long userId) {
        baseMapper.deleteById(userId);
    }

    @Override
    public void updateByTempId(CartInfo noLoginCartInfo) {
        baseMapper.updateById(noLoginCartInfo);
    }

    @Override
    public void checkCart(String userId, Long skuId, Integer isChecked) {
        CartInfo cartInfo = new CartInfo();
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("sku_id",skuId);
        wrapper.eq("user_id",userId);
        cartInfo.setIsChecked(isChecked);
        baseMapper.update(cartInfo,wrapper);
    }


}
