package com.atguigu.exception;

public class SleepUtils {
    public static void sleep(int second) {
        try {
            Thread.sleep(1000*second);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public static void sleepMillis(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
