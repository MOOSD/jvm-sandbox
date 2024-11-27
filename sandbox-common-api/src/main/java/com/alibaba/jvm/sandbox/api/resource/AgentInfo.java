package com.alibaba.jvm.sandbox.api.resource;

/**
 * 记录当前agent的状态
 */
public interface AgentInfo {

    /**
     * 记录鹰眼服务器是否可用
     * @return
     */
    Boolean hKServiceIsAvailable();

    /**
     * 获取当前agent的实例ID
     * @return
     */
    String getInstanceId();
}
