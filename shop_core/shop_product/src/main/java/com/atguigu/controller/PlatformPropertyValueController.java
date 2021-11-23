package com.atguigu.controller;


import com.atguigu.entity.PlatformPropertyKey;
import com.atguigu.entity.PlatformPropertyValue;
import com.atguigu.result.RetVal;
import com.atguigu.service.PlatformPropertyKeyService;
import com.atguigu.service.PlatformPropertyValueService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 属性值表 前端控制器
 * </p>
 *
 * @author zhangqiang
 * @since 2021-10-27
 */
@RestController
@RequestMapping("/product")
public class PlatformPropertyValueController {

    @Autowired
    PlatformPropertyValueService propertyValueService;

    @Autowired
    PlatformPropertyKeyService propertyKeyService;

    //product/getPropertyValueByPropertyKeyId/4
    @GetMapping("/getPropertyValueByPropertyKeyId/{propertyKeyId}")
    public RetVal getPropertyValueByPropertyKeyId(@PathVariable Long propertyKeyId){
        QueryWrapper<PlatformPropertyValue> wrapper = new QueryWrapper<>();
        wrapper.eq("property_key_id",propertyKeyId);
        List<PlatformPropertyValue> list = propertyValueService.list(wrapper);
        return RetVal.ok(list);
    }
    ///savePlatformProperty
    @PostMapping("/savePlatformProperty")
    public RetVal savePlatformProperty(@RequestBody PlatformPropertyKey platformPropertyKey){

        propertyKeyService.savePlatformProperty(platformPropertyKey);
        return RetVal.ok();
    }
    //根据skuId查询平台属性信息
    @GetMapping("/getPlatformPropertyBySkuId/{skuId}")
    public List<PlatformPropertyKey> getPlatformPropertyBySkuId(@PathVariable Long skuId){
        return propertyKeyService.getPlatformPropertyBySkuId(skuId);
    }
}

