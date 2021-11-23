package com.atguigu.service;

import com.atguigu.search.SearchParam;
import com.atguigu.search.SearchResponseVo;

public interface SearchService {
    void onSale(Long skuId);

    void offSale(Long skuId);

    void incrHotScore(Long skuId);

    SearchResponseVo searchProduct(SearchParam searchParam);
}
