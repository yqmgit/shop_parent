package com.atguigu;

import org.springframework.util.DigestUtils;

public class MD5Test {
    public static void main(String[] args) {
        System.out.println( DigestUtils.md5DigestAsHex("123456".getBytes()));
    }
}
