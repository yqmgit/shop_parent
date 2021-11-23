package com.atguigu.service;

import com.atguigu.entity.BaseBrand;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 品牌表 服务类
 * </p>
 *
 * @author zhangqiang
 * @since 2021-10-29
 */
public interface BaseBrandService extends IService<BaseBrand> {

    void setNum();

}
