package com.alibaba.jvm.sandbox.core.server;


import cn.newgrand.ck.constant.ApiPathConstant;
import cn.newgrand.ck.entity.RegisterRequest;
import com.alibaba.jvm.sandbox.api.tools.HkUtils;
import com.alibaba.jvm.sandbox.api.tools.HttpClientUtil;
import com.alibaba.jvm.sandbox.core.CoreConfigure;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 鹰眼的agent注册器
 */
public class HkAgentRegistrar {

    Logger logger = LoggerFactory.getLogger(getClass());
    private final CoreConfigure configure;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();


    public HkAgentRegistrar(CoreConfigure cfg) {
        this.configure = cfg;
    }

    /**
     * 注册agent到鹰眼
     * @return
     */
    public boolean register(){
        if (!checkConfig()){
            return false;
        }
        String url = HkUtils.getUrl(configure.getHkServerIp(), configure.getHkServerPort(), ApiPathConstant.REGISTER_URL);

        RegisterRequest registerRequest = new RegisterRequest();
        try (CloseableHttpResponse closeableHttpResponse = HttpClientUtil.PostByJson(url, registerRequest)){
            // 请求成功后执行健康状态推送
            if(200 == closeableHttpResponse.getStatusLine().getStatusCode()){
                HealthStatusPush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    /**
     * 健康状态推送
     */
    public void HealthStatusPush(){

        Integer healthCheckCycle = configure.getHkHealthCheckCycle();
        if (0 == healthCheckCycle){
            return ;
        }

        String url = HkUtils.getUrl(configure.getHkServerIp(), configure.getHkServerPort(), ApiPathConstant.REGISTER_URL);
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try (CloseableHttpResponse closeableHttpResponse = HttpClientUtil.GetByQueryParam(url, null)) {
                logger.info("心跳响应码:{}",closeableHttpResponse.getStatusLine().getStatusCode());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, 0, healthCheckCycle, TimeUnit.SECONDS);

    }

    /**
     * 检查配置
     */
    private boolean checkConfig(){
        if (ObjectUtils.allNotNull(this.configure.getServerIp(), this.configure.getServerPort())) {
            logger.warn("鹰眼服务端信息错误 {}",this.configure.getServerIp() + this.configure.getServerPort());
            return false;
        }
        return true;
    }





}
