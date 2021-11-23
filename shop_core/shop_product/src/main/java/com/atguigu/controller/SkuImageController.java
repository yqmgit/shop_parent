package com.atguigu.controller;


import com.atguigu.service.BaseBrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 库存单元图片表 前端控制器
 * </p>
 *
 * @author zhangqiang
 * @since 2021-11-01
 */
@RestController
@RequestMapping("/product")
public class SkuImageController {
    @Autowired
    private BaseBrandService brandService;

    @RequestMapping("/setNum")
    public String setNum() {
        brandService.setNum();
        return "success";
    }


}

