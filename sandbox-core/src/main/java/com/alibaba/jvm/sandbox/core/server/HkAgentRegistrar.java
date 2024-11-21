package com.alibaba.jvm.sandbox.core.server;


import cn.newgrand.ck.constant.ApiPathConstant;

import cn.newgrand.ck.entity.request.AgentStatusRequest;
import cn.newgrand.ck.entity.request.DeregisterRequest;
import cn.newgrand.ck.entity.request.JVMInfoRequest;
import cn.newgrand.ck.entity.request.RegisterRequest;
import cn.newgrand.ck.entity.vo.RegisterResponseVO;
import co.elastic.clients.elasticsearch.watcher.ResponseContentType;
import com.alibaba.jvm.sandbox.api.tools.HkUtils;
import com.alibaba.jvm.sandbox.api.tools.HttpClientUtil;
import com.alibaba.jvm.sandbox.core.CoreConfigure;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
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
     *
     */
    public void register() throws Exception {
        checkConfig();
        // 构建请求
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setInstanceId(configure.getInstanceId());
        registerRequest.setAgentName(configure.getAgentName());
        registerRequest.setArtifactId(configure.getArtifactId());
        registerRequest.setGroupId(configure.getGroupId());
        registerRequest.setEnvName(configure.getAgentEnvName());
        registerRequest.setRepoRemoteUrl(configure.getGitRemoteUrl());
        registerRequest.setRepoBranch(configure.getGitBranch());
        registerRequest.setRepoCommitId(configure.getGitCommitId());
        registerRequest.setRepoCommitMessage(configure.getGitCommitMessage());
        registerRequest.setProjectBuildTime(configure.getBuildTime());
        registerRequest.setHealthCheckCycle(configure.getHkHealthCheckCycle());
        registerRequest.setHostName(configure.getHostName());
        registerRequest.setJvmInfo(getJVMInfo());



        String url = HkUtils.getUrl(configure.getHkServerIp(), configure.getHkServerPort(), ApiPathConstant.REGISTER_URL);
        RegisterResponseVO registerResponseVO = HttpClientUtil.postByJson(url, registerRequest, RegisterResponseVO.class);
        if (Objects.isNull(registerResponseVO) || !Boolean.TRUE.equals(registerResponseVO.getSuccess())){
            logger.error("注册失败:{}",registerResponseVO);
            throw new RuntimeException("注册失败");
        }
        logger.info("agent注册成功:{}", registerResponseVO.getSuccess());
        // 开启健康检查
        healthStatusPush();
        // 注册关闭钩子
//        logger.info("注册时的类加载器:{}",this.getClass().getClassLoader().getClass().getName());
//        logger.info("注册时的线程上下文加载器:{}",Thread.currentThread().getContextClassLoader().getClass().getName());
        Runtime.getRuntime().addShutdownHook(new Thread(this::deregister));

    }

    /**
     * 注销
     */
    public void deregister() {
        String url = HkUtils.getUrl(configure.getHkServerIp(), configure.getHkServerPort(), ApiPathConstant.DEREGISTER_URL);
        DeregisterRequest deregisterRequest = new DeregisterRequest();
        deregisterRequest.setInstanceId(configure.getInstanceId());
        try {
            HttpClientUtil.postByJson(url, deregisterRequest);
        } catch (IOException e) {
            logger.error("agent注销异常:",e);
        }
    }

    /**
     * 健康状态推送
     */
    private void healthStatusPush(){

        Integer healthCheckCycle = configure.getHkHealthCheckCycle();
        if (0 == healthCheckCycle){
            return ;
        }

        String url = HkUtils.getUrl(configure.getHkServerIp(), configure.getHkServerPort(), ApiPathConstant.HEALTH_CHECK_URL);
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            AgentStatusRequest agentStatusRequest = new AgentStatusRequest();
            agentStatusRequest.setTimeStamp(System.currentTimeMillis());
            agentStatusRequest.setInstanceId(configure.getInstanceId());
            try {
                Boolean success = HttpClientUtil.postByJson(url, agentStatusRequest, Boolean.class);
            } catch (IOException e) {
                logger.error("心跳无响应",e);
            }
        }, healthCheckCycle, healthCheckCycle, TimeUnit.SECONDS);

    }

    /**
     * 检查配置
     */
    private void checkConfig(){
        if (ObjectUtils.anyNull(this.configure.getHkServerIp(), this.configure.getHkServerPort())) {
            logger.warn("鹰眼服务端信息错误 serve:{},port:{} ", this.configure.getHkServerIp() , this.configure.getHkServerPort());
            throw new RuntimeException("鹰眼服务端信息错误");
        }
    }

    public JVMInfoRequest getJVMInfo(){
        JVMInfoRequest jvmInfo = new JVMInfoRequest();
        jvmInfo.setJvmName(System.getProperty("java.vm.name"));
        jvmInfo.setJvmVersion(System.getProperty("java.vm.version"));
        jvmInfo.setJvmVendor(System.getProperty("java.vm.vendor"));
        jvmInfo.setJavaHome(System.getProperty("java.home"));
        // 所执行的java类文件的版本 例：61
        jvmInfo.setClassVersion(System.getProperty("java.class.version"));
        // 64位
        // System.getProperty("sun.arch.data.model");
        // 操作系统名称
        jvmInfo.setOsName(System.getProperty("os.name"));
        // 操作系统架构
        jvmInfo.setOsArch(System.getProperty("os.arch"));

        // 获取java启动参数
        // 获取 RuntimeMXBean 实例
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        // 获取 JVM 启动参数
        jvmInfo.setJvmArguments(runtimeMXBean.getInputArguments());
        return jvmInfo;
    }

    public void getRuntimeJVMInfo(){
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        // jvm已经运行的时间
        long uptime = runtimeMXBean.getUptime();
    }







}
