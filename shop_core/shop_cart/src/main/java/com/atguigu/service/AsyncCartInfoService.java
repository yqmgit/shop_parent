package com.atguigu.service;

import com.atguigu.entity.CartInfo;

public interface AsyncCartInfoService {
    void addCart(CartInfo cartInfo);

    void updateCart(CartInfo existCartInfo);

    void deleteByUserId(Long userId);

    void updateByTempId(CartInfo noLoginCartInfo);

    void checkCart(String userId, Long skuId, Integer isChecked);
}
