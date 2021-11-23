package com.atguigu.controller;

import com.atguigu.entity.BaseCategoryView;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuInfo;
import com.atguigu.service.BaseCategoryViewService;
import com.atguigu.service.SkuDetailService;
import com.atguigu.service.SkuInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sku/")
public class SkuDetailController {

    @Autowired
    private SkuDetailService skuDetailService;

    @Autowired
    private BaseCategoryViewService baseCategoryViewService;

    @Autowired
    private SkuInfoService skuInfoService;

    //根据skuId获取商品sku的基本信息
    @GetMapping("getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable Long skuId){
        return skuDetailService.getSkuInfo(skuId);
    }
    //根据三级分类id获取sku的分类信息
    @GetMapping("getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id){
        return baseCategoryViewService.getById(category3Id);
    }
    //根据skuId获取所有的spu销售属性和该sku勾选的属性
    @GetMapping("getSpuSalePropertyAndSelected/{productId}/{skuId}")
    public List<ProductSalePropertyKey> getSpuSalePropertyAndSelected(@PathVariable Long productId, @PathVariable Long skuId){
        return skuDetailService.getSpuSalePropertyAndSelected(productId,skuId);
    }

    //根据skuId获取sku的实时价格
    @GetMapping("getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId){
        SkuInfo skuInfo = skuInfoService.getById(skuId);
        return skuInfo.getPrice();
    }
    //查询销售属性组合所对应skuId的对应关系
    @GetMapping("getSalePropertyAndSkuIdMapping/{productId}")
    public Map getSalePropertyAndSkuIdMapping(@PathVariable Long productId){
        return skuDetailService.getSalePropertyAndSkuIdMapping(productId);
    }

}
