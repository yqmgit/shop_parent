package com.atguigu.controller;


import com.atguigu.client.ProductFeignClient;
import com.atguigu.client.SearchFeignClient;
import com.atguigu.constant.MqConst;
import com.atguigu.entity.SkuInfo;
import com.atguigu.result.RetVal;
import com.atguigu.service.SkuInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 库存单元表 前端控制器
 * </p>
 *
 * @author zhangqiang
 * @since 2021-11-01
 */
@RestController
@RequestMapping("/product")
public class SkuInfoController {

    @Autowired
    SkuInfoService skuInfoService;
    @Autowired
    SearchFeignClient searchFeignClient;
    @Autowired
    RabbitTemplate rabbitTemplate;

    //http://127.0.0.1/product/querySkuInfoByPage/1/10

    @GetMapping("querySkuInfoByPage/{pageNum}/{pageSize}")
    public RetVal querySkuInfoByPage(@PathVariable Long pageNum,
                                     @PathVariable Long pageSize){
        Page<SkuInfo> page = new Page<>(pageNum, pageSize);
        QueryWrapper<SkuInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("id");
        skuInfoService.page(page,queryWrapper);
        return RetVal.ok(page);
    }

    //http://127.0.0.1/product/offSale/24
    //用rabbit优化商品上下架
    //下架
    @GetMapping("offSale/{skuId}")
    public RetVal offSale(@PathVariable Long skuId){

        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(0);
        skuInfoService.updateById(skuInfo);
        //searchFeignClient.offSale(skuId);
        rabbitTemplate.convertAndSend(MqConst.ON_OFF_SALE_EXCHANGE,MqConst.OFF_SALE_ROUTING_KEY,skuId);
        return RetVal.ok();
    }
    //上架
    //http://127.0.0.1/product/onSale/38
    @GetMapping("onSale/{skuId}")
    public RetVal onSale(@PathVariable Long skuId){

        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(1);
        skuInfoService.updateById(skuInfo);
        //searchFeignClient.onSale(skuId);
        rabbitTemplate.convertAndSend(MqConst.ON_OFF_SALE_EXCHANGE,MqConst.ON_SALE_ROUTING_KEY,skuId);
    return RetVal.ok();
    }


    //保存sku
    @PostMapping("saveSkuInfo")
    public RetVal saveSkuInfo(@RequestBody SkuInfo skuInfo){
        skuInfoService.saveSkuInfo(skuInfo);
        return RetVal.ok();
    }

}

