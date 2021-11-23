package com.atguigu.mapper;

import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * <p>
 * spu销售属性 Mapper 接口
 * </p>
 *
 * @author zhangqiang
 * @since 2021-10-30
 */
public interface ProductSalePropertyKeyMapper extends BaseMapper<ProductSalePropertyKey> {

    List<ProductSalePropertyKey> querySalePropertyByProductId(Long productId);

    List<ProductSalePropertyKey> getSpuSalePropertyAndSelected(Long productId, Long skuId);
}
