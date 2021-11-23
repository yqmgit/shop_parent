package com.atguigu.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
//指明该注解只能放在哪里
@Target(ElementType.METHOD)
//定义该注解的生命周期
@Retention(RetentionPolicy.RUNTIME)
public @interface ShopCache {
    //定义一个前缀  目的：区分缓存数据信息
    String prefix() default "cache";
}
