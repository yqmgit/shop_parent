package com.atguigu.service.impl;

import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.atguigu.entity.*;
import com.atguigu.mapper.SkuInfoMapper;
import com.atguigu.service.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 库存单元表 服务实现类
 * </p>
 *
 * @author zhangqiang
 * @since 2021-11-01
 */
@Service
@Transactional
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoMapper, SkuInfo> implements SkuInfoService {

    @Autowired
    SkuSalePropertyValueService skuSalePropertyService;
    @Autowired
    SkuImageService skuImageService;
    @Autowired
    SkuPlatformPropertyValueService skuPlatformPropertyService;

    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {
        //保存基础信息
        baseMapper.insert(skuInfo);
        Long skuId = skuInfo.getId();
        //2.sku的平台属性sku_platform_property_value
        List<SkuPlatformPropertyValue> skuPlatformPropertyList = skuInfo.getSkuPlatformPropertyValueList();
        if(!CollectionUtils.isEmpty(skuPlatformPropertyList)){
            for (SkuPlatformPropertyValue skuPlatformPropertyValue : skuPlatformPropertyList) {
                //该平台属性属于那个sku
                skuPlatformPropertyValue.setSkuId(skuId);
            }
            skuPlatformPropertyService.saveBatch(skuPlatformPropertyList);
        }
        //3.sku的销售属性sku_sale_property_value
        List<SkuSalePropertyValue> skuSalePropertyValueList = skuInfo.getSkuSalePropertyValueList();
        if(!CollectionUtils.isEmpty(skuSalePropertyValueList)){
            for (SkuSalePropertyValue skuSalePropertyValue : skuSalePropertyValueList) {
                //归属那个skui
                skuSalePropertyValue.setSkuId(skuId);
                //归属那个spu
                skuSalePropertyValue.setProductId(skuInfo.getProductId());
            }
            skuSalePropertyService.saveBatch(skuSalePropertyValueList);
        }
        //4.sku勾选的图片sku_image
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if(!CollectionUtils.isEmpty(skuImageList)){
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuId);
            }
            skuImageService.saveBatch(skuImageList);
        }
    }
}
