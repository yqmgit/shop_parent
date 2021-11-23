package com.atguigu.controller;

import com.atguigu.result.RetVal;
import com.atguigu.search.Product;
import com.atguigu.search.SearchParam;
import com.atguigu.search.SearchResponseVo;
import com.atguigu.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/search")
public class SearchController {
    @Autowired
    ElasticsearchRestTemplate elasticsearchRestTemplate;
    @Autowired
    SearchService searchService;

    //创建索引和映射
    @GetMapping("/createIndex")
    public RetVal createIndex(){
        elasticsearchRestTemplate.createIndex(Product.class);
        elasticsearchRestTemplate.putMapping(Product.class);
        return RetVal.ok();
    }

    //商品的上架
    @GetMapping("/onSale/{skuId}")
    public RetVal onSale(@PathVariable Long skuId){
        searchService.onSale(skuId);
        return RetVal.ok();
    }
    //商品的下架
    @GetMapping("/offSale/{skuId}")
    public RetVal offSale(@PathVariable Long skuId){
        searchService.offSale(skuId);
        return RetVal.ok();
    }
    //商品热度
    @GetMapping("incrHotScore/{skuId}")
    public RetVal incrHotScore(@PathVariable Long skuId){
        searchService.incrHotScore(skuId);
        return RetVal.ok();
    }

    //商品的搜索功能
    @PostMapping
    public RetVal searchProduct(@RequestBody SearchParam searchParam){
        SearchResponseVo searchResponseVo = searchService.searchProduct(searchParam);
        return RetVal.ok(searchResponseVo);
    }
}
