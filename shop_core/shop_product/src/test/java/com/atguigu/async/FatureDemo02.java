package com.atguigu.async;

import com.atguigu.exception.SleepUtils;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class FatureDemo02 {
    public static void main(String[] args) throws Exception {
        runAsync();
        System.out.println(Thread.currentThread().getName()+"线程执行");
    }
    //发起一个异步请求
    public static void runAsync(){
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread().getName()+"你好runAsync");
                int a = 1/0;
            }
        }).whenComplete(new BiConsumer<Void, Throwable>() {//不管有没有异常都会执行
            @Override
            public void accept(Void aVoid, Throwable throwable) {
                System.out.println("获取上面执行之后的返回值"+aVoid);
                System.out.println("whenComplete接受到上面发生的异常"+throwable);
            }
        }).exceptionally(new Function<Throwable, Void>() {//有异常才会执行
            @Override
            public Void apply(Throwable throwable) {
                System.out.println("exceptionally接受到上面发生的异常"+throwable);
                return null;
            }
        });
    }
}
