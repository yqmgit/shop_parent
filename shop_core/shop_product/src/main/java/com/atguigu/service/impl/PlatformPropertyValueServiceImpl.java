package com.atguigu.service.impl;

import com.atguigu.entity.PlatformPropertyKey;
import com.atguigu.entity.PlatformPropertyValue;
import com.atguigu.mapper.PlatformPropertyValueMapper;
import com.atguigu.service.PlatformPropertyValueService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 属性值表 服务实现类
 * </p>
 *
 * @author zhangqiang
 * @since 2021-10-27
 */
@Service
public class PlatformPropertyValueServiceImpl extends ServiceImpl<PlatformPropertyValueMapper, PlatformPropertyValue> implements PlatformPropertyValueService {


}
