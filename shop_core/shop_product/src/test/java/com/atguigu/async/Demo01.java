package com.atguigu.async;

import com.atguigu.exception.SleepUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public class Demo01 {
    public static void main(String[] args) throws Exception {
        //runAsync();
        supplyAsync();
        System.out.println(Thread.currentThread().getName()+"线程执行");
    }
    //发起一个异步请求
    public static void runAsync(){
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread().getName()+"你好runAsync");
                SleepUtils.sleep(2);
                System.out.println("另外线程在等待");
            }
        });
    }

    public static void supplyAsync() throws Exception {
        CompletableFuture<String> supplyAsync = CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            public String get() {
                System.out.println(Thread.currentThread().getName()+"你好supplyAsync");
                SleepUtils.sleep(2);
                return "0521";
            }
        });
        System.out.println(Thread.currentThread().getName()+supplyAsync.get());
    }
}
