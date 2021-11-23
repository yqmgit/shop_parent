package com.atguigu.service;

import com.atguigu.entity.ProductSpu;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 商品表 服务类
 * </p>
 *
 * @author zhangqiang
 * @since 2021-10-29
 */
public interface ProductSpuService extends IService<ProductSpu> {

    void saveProductSpu(ProductSpu productSpu);

}
