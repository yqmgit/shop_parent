package com.atguigu.service;

import com.atguigu.entity.CartInfo;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * <p>
 * 购物车表 用户登录系统时更新冗余 服务类
 * </p>
 *
 * @author zhangqiang
 * @since 2021-11-10
 */
public interface CartInfoService extends IService<CartInfo> {

    void addToCart(String userId, Long skuId, Integer skuNum);

    List<CartInfo> getCartList(String userId, String userTempId);

    void deleteCart(Long skuId);

    void checkCart(Long skuId, Integer isChecked ,HttpServletRequest request);

    //更新缓存信息
    List<CartInfo> queryFromDbToRedis(String userTempId);

    List<CartInfo> getSelectedProduct(String userId);
}
