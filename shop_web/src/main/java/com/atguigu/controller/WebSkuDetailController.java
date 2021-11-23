package com.atguigu.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.client.ProductFeignClient;
import com.atguigu.entity.BaseCategoryView;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;


import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import com.atguigu.executor.MyExecutor;

@Controller
public class WebSkuDetailController {
    @Autowired
    private ProductFeignClient productFeignClient;

    //编写访问的控制器！
    @RequestMapping("{skuId}.html")
    public String getSkuDetail(@PathVariable Long skuId, Model model){
        Map<String,Object> dataMap =new HashMap<>();


        //a.根据skuId获取商品sku的基本信息,需要返回值
        CompletableFuture<SkuInfo> skuInfoFuture = CompletableFuture.supplyAsync(()->{
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            dataMap.put("skuInfo",skuInfo);
            return skuInfo;
        });
        //b.根据三级分类id获取sku的分类信息
        CompletableFuture<Void> categoryViewFuture = skuInfoFuture.thenAcceptAsync(skuInfo->{
            Long category3Id = skuInfo.getCategory3Id();
            BaseCategoryView categoryView = productFeignClient.getCategoryView(category3Id);
            dataMap.put("categoryView",categoryView);
        },MyExecutor.getInstance());

        //c.根据skuId获取sku的实时价格，无需返回值
        CompletableFuture<Void> skuPriceFuture = CompletableFuture.runAsync(()->{
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            dataMap.put("price",skuPrice);
        });

        //d.根据skuId获取所有的spu销售属性与该sku所勾选的销售属性
        CompletableFuture<Void> spuSalePropertyFuture = skuInfoFuture.thenAcceptAsync(skuInfo -> {
            Long productId = skuInfo.getProductId();
            List<ProductSalePropertyKey> spuSalePropertyList = productFeignClient.getSpuSalePropertyAndSelected(productId, skuId);
            dataMap.put("spuSalePropertyList",spuSalePropertyList);
        },MyExecutor.getInstance());

        //e.查询销售属性组合所对于skuId的对于关系
        CompletableFuture<Void> propertyAndSkuIdMapFuture = skuInfoFuture.thenAcceptAsync(skuInfo -> {
            Map salePropertyAndSkuIdMap = productFeignClient.getSalePropertyAndSkuIdMapping(skuInfo.getProductId());
            dataMap.put("salePropertyValueIdJson", JSON.toJSONString(salePropertyAndSkuIdMap));
        },MyExecutor.getInstance());


        CompletableFuture.allOf(
                skuInfoFuture,
                categoryViewFuture,
                skuPriceFuture,
                spuSalePropertyFuture,
                propertyAndSkuIdMapFuture
        ).join();
        model.addAllAttributes(dataMap);
        return "detail/index";




//        //a.根据skuId获取商品sku的基本信息
//        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
//        dataMap.put("skuInfo",skuInfo);
//        //b.根据三级分类id获取sku的分类信息
//        Long category3Id = skuInfo.getCategory3Id();
//        BaseCategoryView categoryView = productFeignClient.getCategoryView(category3Id);
//        dataMap.put("categoryView",categoryView);
//        //c.根据skuId获取sku的实时价格
//        BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
//        dataMap.put("price",skuPrice);
//        //d.根据skuId获取所有的spu销售属性与该sku所勾选的销售属性
//        Long productId = skuInfo.getProductId();
//        List<ProductSalePropertyKey> spuSalePropertyList = productFeignClient.getSpuSalePropertyAndSelected(productId, skuId);
//        dataMap.put("spuSalePropertyList",spuSalePropertyList);
//        //e.查询销售属性组合所对于skuId的对于关系
//        Map salePropertyAndSkuIdMap = productFeignClient.getSalePropertyAndSkuIdMapping(productId);
//        dataMap.put("salePropertyValueIdJson", JSON.toJSONString(salePropertyAndSkuIdMap));
//
//
//        model.addAllAttributes(dataMap);
//        return "detail/index";
    }
}
