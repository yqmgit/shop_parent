package com.atguigu.mapper;

import com.atguigu.entity.SkuSalePropertyValue;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * sku销售属性值 Mapper 接口
 * </p>
 *
 * @author zhangqiang
 * @since 2021-11-01
 */
public interface SkuSalePropertyValueMapper extends BaseMapper<SkuSalePropertyValue> {

    List<Map> getSalePropertyAndSkuIdMapping(Long productId);
}
