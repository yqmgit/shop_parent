package com.atguigu.async;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class FatureDemo03 {
    public static void main(String[] args) throws Exception {
        runAsync1();
        runAsync2();
        System.out.println(Thread.currentThread().getName()+"线程执行");
    }
    //发起一个异步请求
    public static void runAsync1(){
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread().getName()+"你好runAsync1");
            }
        }).whenComplete(new BiConsumer<Void, Throwable>() {//发生异步后由main线程执行
            @Override
            public void accept(Void aVoid, Throwable throwable) {
                System.out.println(Thread.currentThread().getName()+"获取上面执行之后的返回值"+aVoid);
            }
        });
    }

    //发起一个异步请求
    public static void runAsync2(){
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread().getName()+"你好runAsync2");
            }
        }).whenCompleteAsync(new BiConsumer<Void, Throwable>() {//发生异步后由线程池执行
            @Override
            public void accept(Void aVoid, Throwable throwable) {
                System.out.println(Thread.currentThread().getName()+"获取上面执行之后的返回值"+aVoid);
            }
        });
    }
}
