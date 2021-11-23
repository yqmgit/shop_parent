package com.atguigu.controller;


import com.atguigu.result.RetVal;
import com.atguigu.service.ProductImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 商品图片表 前端控制器
 * </p>
 *
 * @author zhangqiang
 * @since 2021-10-30
 */
@RestController
@RequestMapping("/product")
public class ProductImageController {

    @Autowired
    ProductImageService productImageService;

/*    //http://127.0.0.1/product/queryProductImageByProductId/12
    @GetMapping("queryProductImageByProductId/{product_id}")
    public RetVal queryProductImageByProductId(@PathVariable Long productId){

        return RetVal.ok();
    }*/

}

