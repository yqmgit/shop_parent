package com.atguigu.service.impl;

import com.atguigu.entity.PlatformPropertyKey;
import com.atguigu.entity.PlatformPropertyValue;

import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.mapper.PlatformPropertyKeyMapper;
import com.atguigu.service.PlatformPropertyKeyService;
import com.atguigu.service.PlatformPropertyValueService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 属性表 服务实现类
 * </p>
 *
 * @author zhangqiang
 * @since 2021-10-27
 */
@Service
@Transactional
public class PlatformPropertyKeyServiceImpl extends ServiceImpl<PlatformPropertyKeyMapper, PlatformPropertyKey> implements PlatformPropertyKeyService {


    @Autowired
    PlatformPropertyValueService propertyValueService;

    @Override
    public List<PlatformPropertyKey> getPropertyByCategoryId(Long category1Id, Long category2Id, Long category3Id) {

        return baseMapper.getPropertyByCategoryId(category1Id,category2Id, category3Id);
    }


    @Override
    @Transactional
    public void savePlatformProperty(PlatformPropertyKey platformPropertyKey) {
        //判断是否有id，有就修改
        if(platformPropertyKey.getId()!=null){
            baseMapper.updateById(platformPropertyKey);


        }else{
            //没有就添加
            baseMapper.insert(platformPropertyKey);
        }

        //数据库的值删除重新添加
        QueryWrapper<PlatformPropertyValue> wrapper = new QueryWrapper<>();
        wrapper.eq("property_key_id",platformPropertyKey.getId());
        propertyValueService.remove(wrapper);


        List<PlatformPropertyValue> propertyValueList = platformPropertyKey.getPropertyValueList();
        for (PlatformPropertyValue platformPropertyValue : propertyValueList) {
            platformPropertyValue.setPropertyKeyId(platformPropertyKey.getId());
        }

        propertyValueService.saveBatch(propertyValueList);
    }

    @Override
    public List<PlatformPropertyKey> getPlatformPropertyBySkuId(Long skuId) {
        return baseMapper.getPlatformPropertyBySkuId(skuId);
    }
}
