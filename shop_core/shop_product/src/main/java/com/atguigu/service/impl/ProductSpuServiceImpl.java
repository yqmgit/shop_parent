package com.atguigu.service.impl;

import com.atguigu.entity.ProductImage;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.ProductSalePropertyValue;
import com.atguigu.entity.ProductSpu;
import com.atguigu.mapper.ProductSpuMapper;
import com.atguigu.service.ProductImageService;
import com.atguigu.service.ProductSalePropertyKeyService;
import com.atguigu.service.ProductSalePropertyValueService;
import com.atguigu.service.ProductSpuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * <p>
 * 商品表 服务实现类
 * </p>
 *
 * @author zhangqiang
 * @since 2021-10-29
 */
@Service
public class ProductSpuServiceImpl extends ServiceImpl<ProductSpuMapper, ProductSpu> implements ProductSpuService {

    @Autowired
    ProductSalePropertyKeyService salePropertyKeyService;

    @Autowired
    ProductSalePropertyValueService salePropertyValueService;

    @Autowired
    ProductImageService productImageService;

    @Override
    public void saveProductSpu(ProductSpu productSpu) {

        //保存spu的基本信息
        baseMapper.insert(productSpu);

        //保存spu的图片信息
        List<ProductImage> productImageList = productSpu.getProductImageList();
        if (!CollectionUtils.isEmpty(productImageList)){
            for (ProductImage productImage : productImageList) {
                productImage.setProductId(productSpu.getId());
            }
            productImageService.saveBatch(productImageList);
        }
        //保存spu的商品属性
        List<ProductSalePropertyKey> salePropertyKeyList = productSpu.getSalePropertyKeyList();
        if (!CollectionUtils.isEmpty(salePropertyKeyList)){
            for (ProductSalePropertyKey productSalePropertyKey : salePropertyKeyList) {
                productSalePropertyKey.setProductId(productSpu.getId());


                List<ProductSalePropertyValue> salePropertyValueList = productSalePropertyKey.getSalePropertyValueList();
                if (!CollectionUtils.isEmpty(salePropertyValueList)){
                    for (ProductSalePropertyValue productSalePropertyValue : salePropertyValueList) {
                        productSalePropertyValue.setProductId(productSpu.getId());
                        //该销售属性值属于哪个销售属性key
                        productSalePropertyValue.setSalePropertyKeyName(productSalePropertyKey.getSalePropertyKeyName());
                    }
                    salePropertyValueService.saveBatch(salePropertyValueList);
                }
            }
            salePropertyKeyService.saveBatch(salePropertyKeyList);
        }

    }
}
