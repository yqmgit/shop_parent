package com.atguigu.controller;


import com.alibaba.fastjson.JSONObject;
import com.atguigu.entity.BaseCategory1;
import com.atguigu.entity.BaseCategory2;
import com.atguigu.entity.BaseCategory3;
import com.atguigu.result.RetVal;
import com.atguigu.service.BaseCategory1Service;
import com.atguigu.service.BaseCategory2Service;
import com.atguigu.service.BaseCategory3Service;
import com.atguigu.service.BaseCategoryViewService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 一级分类表 前端控制器
 * </p>
 *
 * @author zhangqiang
 * @since 2021-10-27
 */
@RestController
@RequestMapping("/product")
//http://127.0.0.1/product/getCategory1
public class BaseCategoryController {

    @Autowired
    BaseCategory1Service baseCategory1Service;

    @Autowired
    BaseCategory2Service baseCategory2Service;

    @Autowired
    BaseCategory3Service baseCategory3Service;
    @Autowired
    BaseCategoryViewService categoryViewService;


    @GetMapping("/getCategory1")
    public RetVal getCategory1(){

        List<BaseCategory1> category1List = baseCategory1Service.list(null);
        return RetVal.ok(category1List);
    }
    @GetMapping("/getCategory2/{category1Id}")
    public RetVal getCategory2(@PathVariable Long category1Id){

        QueryWrapper<BaseCategory2> wrapper = new QueryWrapper<>();
        wrapper.eq("category1_id",category1Id );
        List<BaseCategory2> category2List = baseCategory2Service.list(wrapper);

        return RetVal.ok(category2List);
    }
    @GetMapping("/getCategory3/{category2Id}")
    public RetVal getCategory3(@PathVariable Long category2Id){

        QueryWrapper<BaseCategory3> wrapper = new QueryWrapper<>();
        wrapper.eq("category2_id",category2Id );
        List<BaseCategory3> category3List = baseCategory3Service.list(wrapper);

        return RetVal.ok(category3List);
    }

    //查询首页分类信息
    @GetMapping("/getIndexCategoryInfo")
    public RetVal getIndexCategoryInfo(){
        List<JSONObject> categoryInfo = categoryViewService.getIndexCategoryInfo();
        return RetVal.ok(categoryInfo);
    }


}

