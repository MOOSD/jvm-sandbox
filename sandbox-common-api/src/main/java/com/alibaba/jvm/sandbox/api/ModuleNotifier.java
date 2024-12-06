package com.alibaba.jvm.sandbox.api;

/**
 * 模块通知器，模块可以选择性的实现通知器，在Agent状态发生变化后，通知器会回调对应方法
 */
public interface ModuleNotifier {

    /**
     * 模块通知器
     * @param available 鹰眼服务器是否可用
     */
    default void hkServerStateChange(boolean available){

    }

    default void agentStop(){

    }
}
