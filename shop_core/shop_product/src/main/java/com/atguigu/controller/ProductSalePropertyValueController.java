package com.atguigu.controller;


import com.atguigu.entity.ProductImage;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuInfo;
import com.atguigu.result.RetVal;
import com.atguigu.service.ProductImageService;
import com.atguigu.service.ProductSalePropertyKeyService;
import com.atguigu.service.ProductSalePropertyValueService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * spu销售属性值 前端控制器
 * </p>
 *
 * @author zhangqiang
 * @since 2021-10-30
 */
@RestController
@RequestMapping("/product")
public class ProductSalePropertyValueController {
    @Autowired
    ProductSalePropertyKeyService salePropertyKeyService;

    @Autowired
    ProductImageService productImageService;

//http://127.0.0.1/product/querySalePropertyByProductId/12
    //根据商品的spu 查询销售属性
    @GetMapping("querySalePropertyByProductId/{productId}")
    public RetVal querySalePropertyByProductId(@PathVariable Long productId){
        List<ProductSalePropertyKey> salePropertyKeyList = salePropertyKeyService.querySalePropertyByProductId(productId);
        return RetVal.ok(salePropertyKeyList);
    }
    //2.根据商品spu的id查询productImage里面的图片信息 http://10.211.55.98/product/queryProductImageByProductId/16
    @GetMapping("queryProductImageByProductId/{productId}")
    public RetVal queryProductImageByProductId(@PathVariable Long productId) {
        QueryWrapper<ProductImage> wrapper = new QueryWrapper<>();
        wrapper.eq("product_id", productId);
        List<ProductImage> productImageList = productImageService.list(wrapper);
        return RetVal.ok(productImageList);
    }
}

