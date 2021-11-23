package com.atguigu.controller;


import com.atguigu.entity.BaseSaleProperty;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.result.RetVal;
import com.atguigu.service.BaseSalePropertyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 基本销售属性表 前端控制器
 * </p>
 *
 * @author zhangqiang
 * @since 2021-10-30
 */
@RestController
@RequestMapping("/product")
public class BaseSalePropertyController {

    @Autowired
    BaseSalePropertyService salePropertyService;

    //http://127.0.0.1/product/queryAllSaleProperty
    @GetMapping("queryAllSaleProperty")
    public RetVal queryAllSaleProperty(){
        List<BaseSaleProperty> list = salePropertyService.list(null);
        return RetVal.ok(list);
    }
}

