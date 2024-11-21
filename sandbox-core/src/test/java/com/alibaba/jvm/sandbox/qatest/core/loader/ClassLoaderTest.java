package com.alibaba.jvm.sandbox.qatest.core.loader;

public class ClassLoaderTest {

    public static void main(String[] args) throws ClassNotFoundException {
        //获取全局的应用类加载器*
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

        //获取线程上下文的类加载器
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();


        //获取加载此Class对象的类加载器
        ClassLoader thisLoader = ClassLoaderTest.class.getClassLoader();

        //在此类中加载一个类路径下的类，查看其是用哪个类加载加载的
        ClassLoader otherLoader = Class.forName("com.alibaba.jvm.sandbox.qatest.core.loader.ClassLoaderTest").getClassLoader();


        //类地址打印
        System.out.println("应用类加载器:"+systemClassLoader);
        System.out.println("线程上下文的类加载器:"+contextClassLoader);
        System.out.println("加载此类所用的类加载器:"+thisLoader);
        System.out.println("此类中加载的类所用的类加载器:"+otherLoader);

    }
}
