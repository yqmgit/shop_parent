package com.atguigu.service;

import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * spu销售属性 服务类
 * </p>
 *
 * @author zhangqiang
 * @since 2021-10-30
 */
public interface ProductSalePropertyKeyService extends IService<ProductSalePropertyKey> {


    List<ProductSalePropertyKey> querySalePropertyByProductId(Long productId);

}
