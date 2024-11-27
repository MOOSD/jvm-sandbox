package com.alibaba.jvm.sandbox.api.resource;

public interface ModuleObserver {
    /**
     * 向所有的模块推送hk服务端的状态
     */
    void hkServerStatePushAllModule(boolean available);
}
