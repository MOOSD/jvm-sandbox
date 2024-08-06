package com.alibaba.jvm.sandbox.agent;

import org.springframework.boot.loader.LaunchedURLClassLoader;

import java.net.URL;


public class CompoundableClassLoader extends LaunchedURLClassLoader{
    public CompoundableClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }
}
