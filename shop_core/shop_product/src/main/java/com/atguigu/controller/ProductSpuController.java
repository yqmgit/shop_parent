package com.atguigu.controller;


import com.atguigu.entity.ProductSpu;
import com.atguigu.result.RetVal;
import com.atguigu.service.ProductSpuService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 商品表 前端控制器
 * </p>
 *
 * @author zhangqiang
 * @since 2021-10-29
 */
@RestController
@RequestMapping("/product")
public class ProductSpuController {
    @Autowired
    ProductSpuService spuService;

    @GetMapping("queryProductSpuByPage/{pageNum}/{pageSize}/{category3Id}")
    public RetVal queryProductSpuByPage(@PathVariable Long pageNum,
                                        @PathVariable Long pageSize,
                                        @PathVariable Long category3Id){
        Page<ProductSpu> page = new Page<>(pageNum, pageSize);
        QueryWrapper<ProductSpu> wrapper = new QueryWrapper<>();
        wrapper.eq("category3_id",category3Id);
        spuService.page(page, wrapper);
        return RetVal.ok(page);
    }

    //http://127.0.0.1/product/saveProductSpu
    @PostMapping("saveProductSpu")
    public RetVal saveProductSpu(@RequestBody ProductSpu productSpu){
        spuService.saveProductSpu(productSpu);
        return RetVal.ok();
    }
    //人间大事，吃喝二字

}

