package com.atguigu.client;


import com.atguigu.entity.*;
import com.atguigu.fallback.ProductFallBack;
import com.atguigu.result.RetVal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
@FeignClient(value = "shop-product",fallback = ProductFallBack.class)
public interface ProductFeignClient {
    //a.根据skuId获取商品sku的基本信息
    @GetMapping("/sku/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable Long skuId);
    //b.根据三级分类id获取sku的分类信息
    @GetMapping("/sku/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id);
    //c.根据skuId获取sku的实时价格
    @GetMapping("/sku/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId);
    //d.根据skuId获取所有的spu销售属性与该sku所勾选的销售属性
    @GetMapping("/sku/getSpuSalePropertyAndSelected/{productId}/{skuId}")
    public List<ProductSalePropertyKey> getSpuSalePropertyAndSelected(@PathVariable Long productId, @PathVariable Long skuId);
    //e.查询销售属性组合所对应skuId的对应关系
    @GetMapping("/sku/getSalePropertyAndSkuIdMapping/{productId}")
    public Map getSalePropertyAndSkuIdMapping(@PathVariable Long productId);
    //获取首页分类信息
    @GetMapping("/product/getIndexCategoryInfo")
    public RetVal getIndexCategoryInfo();
    //根据id查询品牌信息
    @GetMapping("/product/brand/getBrandById/{brandId}")
    public BaseBrand getBrandById1(@PathVariable Long brandId);
    //根据skuId查询平台属性信息
    @GetMapping("/product/getPlatformPropertyBySkuId/{skuId}")
    public List<PlatformPropertyKey> getPlatformPropertyBySkuId(@PathVariable Long skuId);

}
