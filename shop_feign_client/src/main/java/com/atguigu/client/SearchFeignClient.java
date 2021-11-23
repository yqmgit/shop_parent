package com.atguigu.client;

import com.atguigu.result.RetVal;
import com.atguigu.search.SearchParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(value = "shop-search")
public interface SearchFeignClient {
    //商品上架
    @GetMapping("/search/onSale/{skuId}")
    public RetVal onSale(@PathVariable Long skuId);
    //商品下架
    @GetMapping("/search/offSale/{skuId}")
    public RetVal offSale(@PathVariable Long skuId);
    //商品热度
    @GetMapping("/search/incrHotScore/{skuId}")
    public RetVal incrHotScore(@PathVariable Long skuId);

    //商品搜索
    @GetMapping("/search/")
    public RetVal<Map> searchProduct(SearchParam searchParam);
}
