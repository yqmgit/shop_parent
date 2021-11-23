package com.atguigu.controller;


import com.atguigu.entity.BaseBrand;
import com.atguigu.result.RetVal;
import com.atguigu.service.BaseBrandService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.io.FilenameUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * <p>
 * 品牌表 前端控制器
 * </p>
 *
 * @author zhangqiang
 * @since 2021-10-29
 */
@RestController
@RequestMapping("/product/brand")
public class BrandController {

    @Value("${fastdfs.prefix}")
    private String fastdfsPrefix;

    @Autowired
    BaseBrandService brandService;


    //product/brand/queryBrandByPage/1/10
    @GetMapping("/queryBrandByPage/{pageNum}/{pageSize}")
    public RetVal queryBrandByPage(@PathVariable Long pageNum,
                                   @PathVariable Long pageSize){

        Page<BaseBrand> brandPage = new Page<>(pageNum,pageSize);
        QueryWrapper<BaseBrand> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("id");
        IPage<BaseBrand> page = brandService.page(brandPage, wrapper);
        return RetVal.ok(page);
    }

    //添加product/brand/fileUpload
    @PostMapping("/fileUpload")
    public RetVal fileUpload(MultipartFile file) throws Exception {

        //1.需要拿到fastdfs的tracker所在位置
        String configFile = this.getClass().getResource("/tracker.conf").getFile();
        //2.初始化
        ClientGlobal.init(configFile);
        //3.创建一个trackerClient客户端
        TrackerClient trackerClient = new TrackerClient();
        TrackerServer trackerServer = trackerClient.getConnection();
        //4.创建一个storageClient客户端
        StorageClient1 storageClient1 = new StorageClient1(trackerServer, null);

        //5.实现文件上传
        String originalFilename = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalFilename);
        String path = storageClient1.upload_appender_file1(file.getBytes(), extension, null);


        return RetVal.ok(fastdfsPrefix+path);
    }
    //添加品牌
    @PostMapping
    public RetVal save(@RequestBody BaseBrand brand){
        brandService.save(brand);
        return RetVal.ok();
    }
    //http://127.0.0.1/product/brand/4
    //3.根据id查询品牌信息
    @GetMapping("{brandId}")
    public RetVal getBrandById(@PathVariable Long brandId){
        BaseBrand brand = brandService.getById(brandId);
        return RetVal.ok(brand);
    }
    //4.根据id查询品牌信息
    @GetMapping("/getBrandById/{brandId}")
    public BaseBrand getBrandById1(@PathVariable Long brandId){
        BaseBrand brand = brandService.getById(brandId);
        return brand;
    }

    //4.更新品牌信息
    @PutMapping
    public RetVal updateBrand(@RequestBody BaseBrand brand){
        brandService.updateById(brand);
        return RetVal.ok();
    }
    //5.删除品牌信息
    @DeleteMapping("{brandId}")
    public RetVal remove(@PathVariable Long brandId){
        brandService.removeById(brandId);
        return RetVal.ok();
    }

    //http://127.0.0.1/product/brand/getAllBrand
    @GetMapping("getAllBrand")
    public RetVal getAllBrand(){
        QueryWrapper<BaseBrand> wrapper = new QueryWrapper<>();
        List<BaseBrand> baseBrandList = brandService.list(wrapper);
        return RetVal.ok(baseBrandList);
    }

}

