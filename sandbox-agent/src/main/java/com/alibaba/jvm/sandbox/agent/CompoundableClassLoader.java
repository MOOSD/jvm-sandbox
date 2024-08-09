package com.alibaba.jvm.sandbox.agent;

import org.springframework.boot.loader.LaunchedURLClassLoader;

import java.net.URL;


public class CompoundableClassLoader extends LaunchedURLClassLoader{
    private final String toString;
    public CompoundableClassLoader(String namespace, URL coreUrl, ClassLoader parent) {
        super(new URL[]{coreUrl}, parent);
        this.toString = String.format("SandboxClassLoader[namespace=%s;path=%s;]", namespace, coreUrl);

    }
}
