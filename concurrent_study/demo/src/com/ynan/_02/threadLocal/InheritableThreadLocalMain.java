package com.ynan._02.threadLocal;

/**
 * @program: concurrent_study
 * @description:
 * @author: yn
 * @create: 2021-06-21 10:16
 */
public class InheritableThreadLocalMain {

    public static void main(String[] args) {

        Thread thread = Thread.currentThread();

        // 父线程的 InheritableThreadLocal
        ThreadLocal<String> inheritableThreadLocal = new InheritableThreadLocal();
        ThreadLocal threadLocal = new ThreadLocal();

        new Thread();
        inheritableThreadLocal.get();
        new Thread();
        threadLocal.get();

        // 父线程set值
        inheritableThreadLocal.set("hello");

        // 创建子线程的时候
        new Thread(() -> {
            // 子线程获取值
            String s = inheritableThreadLocal.get();
            System.out.println(s);
        }).start();

        // 但是线程创建后，是读取不到父线程再次set进去值的
        inheritableThreadLocal.set("world");
    }
}
