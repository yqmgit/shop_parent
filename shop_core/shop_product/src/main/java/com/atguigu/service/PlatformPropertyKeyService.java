package com.atguigu.service;

import com.atguigu.entity.PlatformPropertyKey;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 属性表 服务类
 * </p>
 *
 * @author zhangqiang
 * @since 2021-10-27
 */
public interface PlatformPropertyKeyService extends IService<PlatformPropertyKey> {

    List<PlatformPropertyKey> getPropertyByCategoryId(Long category1Id, Long category2Id, Long category3Id);

    void savePlatformProperty(PlatformPropertyKey platformPropertyKey);

    List<PlatformPropertyKey> getPlatformPropertyBySkuId(Long skuId);

}
