package com.alibaba.jvm.sandbox.core.server;


import com.alibaba.jvm.sandbox.api.tools.HttpClientUtil;
import com.alibaba.jvm.sandbox.core.CoreConfigure;
import org.apache.http.impl.client.CloseableHttpClient;

/**
 * 服务注册
 */
public class AgentRegistrar {

    private final CoreConfigure configure;


    public AgentRegistrar(CoreConfigure cfg) {
        this.configure = cfg;
    }

    public boolean register(){
        String hkServerIp = configure.getHkServerIp();
        return true;
    }

    /**
     * 健康状态推送
     */
    public void HealthStatusPush(){
        Integer hkHealthCycle = configure.getHkHealthCycle();
        if (0 == hkHealthCycle){
            return ;
        }

    }

}
