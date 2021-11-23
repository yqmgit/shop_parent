package com.atguigu.async;

import com.atguigu.exception.SleepUtils;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class FatureDemo05 {
    public static void main(String[] args) {
        supplyAsync();
        SleepUtils.sleep(10);
    }
    //发起一个异步请求，串行执行
    public static void supplyAsync() {
        CompletableFuture<String> supplyAsync = CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            public String get() {
                System.out.println(Thread.currentThread().getName() + "你好supplyAsync");
                SleepUtils.sleep(2);
                return "0521";
            }
        });
        supplyAsync.thenApply(new Function<String, String>() {
            @Override
            public String apply(String acceptVal) {
                SleepUtils.sleep(2);
                System.out.println(Thread.currentThread().getName() +"第一个线程thenAccept拿到值" + acceptVal);
                return acceptVal+"apply1";
            }
        });
        supplyAsync.thenApply(new Function<String, String>() {
            @Override
            public String apply(String acceptVal) {
                SleepUtils.sleep(2);
                System.out.println(Thread.currentThread().getName() +"第二个线程thenAccept拿到值" + acceptVal);
                return acceptVal+"apply2";
            }
        });
    }
}
