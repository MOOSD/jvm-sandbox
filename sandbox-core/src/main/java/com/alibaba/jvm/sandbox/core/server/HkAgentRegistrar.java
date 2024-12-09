package com.alibaba.jvm.sandbox.core.server;


import cn.newgrand.ck.constant.ApiPathConstant;

import cn.newgrand.ck.entity.request.AgentStatusRequest;
import cn.newgrand.ck.entity.request.DeregisterRequest;
import cn.newgrand.ck.entity.request.JVMInfoRequest;
import cn.newgrand.ck.entity.request.RegisterRequest;
import cn.newgrand.ck.entity.vo.RegisterResponseVO;
import com.alibaba.jvm.sandbox.api.resource.HKServerObserver;
import com.alibaba.jvm.sandbox.api.tools.HkUtils;
import com.alibaba.jvm.sandbox.api.tools.HttpClientUtil;
import com.alibaba.jvm.sandbox.core.CoreConfigure;
import com.alibaba.jvm.sandbox.core.JvmSandbox;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 鹰眼的agent注册器
 */
public class HkAgentRegistrar {
    Logger logger = LoggerFactory.getLogger(getClass());
    private final HKServerObserver hkServerObserver;
    private final CoreConfigure configure;

    private final String instanceId;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    private boolean hkServerIsAvailable = false;

    public HkAgentRegistrar(CoreConfigure cfg, JvmSandbox jvmSandbox) {
        this.configure = cfg;
        // 添加观察者
        hkServerObserver = jvmSandbox;
        // instanceId
        instanceId = jvmSandbox.getInstanceId();
    }


    synchronized public void serviceIsAvailable(boolean available){
        hkServerIsAvailable = available;
        // 通知所有观察者
        hkServerObserver.hkServerAvailableNotify(available);

    }


    /**
     * 激活Register自律行为
     * 注册失败会排除异常
     */
    public void active(){
        checkConfig();
        RegisterResponseVO registerVO = null;
        Exception exception = null;
        try {
            registerVO = requestRegister();
        } catch (Exception e) {
            exception = e;
            logger.info("Agent注册失败",e);
        }
        // 出现异常或者响应失败均视作失败
        if(exception != null || registerVO == null || Boolean.FALSE.equals(registerVO.getSuccess())){
            serviceIsAvailable(false);
            // 开启健康检查 or 自动注册
            healthStatusCheck();
            return;
        }
        serviceIsAvailable(true);

        logger.info("agent注册成功:{}", registerVO.getSuccess());

        // 开启健康检查 和 自动注册
        healthStatusCheck();

    }

    /**
     * 注册接口
     * 返回null、抛出异常、均视作注册失败
     */
    public RegisterResponseVO requestRegister() throws IOException {
        // 构建请求
        RegisterRequest registerRequest = getRegisterRequest();
        String url = HkUtils.getUrl(configure.getHkServerIp(), configure.getHkServerPort(), ApiPathConstant.REGISTER_URL);
        return HttpClientUtil.postByJson(url, registerRequest, RegisterResponseVO.class);
    }

    private RegisterRequest getRegisterRequest() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setInstanceId(instanceId);
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
        return registerRequest;
    }

    /**
     * 注销
     */
    public void deregister() {
        String url = HkUtils.getUrl(configure.getHkServerIp(), configure.getHkServerPort(), ApiPathConstant.DEREGISTER_URL);
        DeregisterRequest deregisterRequest = new DeregisterRequest();
        deregisterRequest.setInstanceId(instanceId);
        try {
            HttpClientUtil.postByJson(url, deregisterRequest);
        } catch (IOException e) {
            logger.error("agent注销异常:",e);
        }
    }

    /**
     * 健康状态推送
     */
    private void healthStatusCheck(){

        Integer healthCheckCycle = configure.getHkHealthCheckCycle();
        if (0 == healthCheckCycle){
            logger.info("agent 未设置agent健康检查周期");
            return ;
        }
        String healthCheckUrl = HkUtils.getUrl(configure.getHkServerIp(), configure.getHkServerPort(), ApiPathConstant.HEALTH_CHECK_URL);
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            Exception healthCheckException = null;
            Boolean healthCheckResult = null;
            try {
                AgentStatusRequest agentStatusRequest = new AgentStatusRequest();
                agentStatusRequest.setTimeStamp(System.currentTimeMillis());
                agentStatusRequest.setInstanceId(instanceId);
                healthCheckResult = HttpClientUtil.postByJson(healthCheckUrl, agentStatusRequest, Boolean.class);
                logger.info("健康检查结果:{}",healthCheckResult);
            } catch (Exception e) {
                healthCheckException = e;
                logger.error("心跳状态推送异常:{}", e.getMessage());
            }
            if( null == healthCheckResult || null !=  healthCheckException ){
                // 出现任何异常则视作鹰眼服务器不可用
                serviceIsAvailable(false);
            }else if(Boolean.FALSE.equals(healthCheckResult)){
                // 健康检查接口返回false时才重新注册
                try {
                    requestRegister();
                    // 否则视作正常响应, 根据返回值判断是否重新注册
                    serviceIsAvailable(true);
                } catch (Exception e) {
                    logger.warn("自动注册失败",e);
                }
            }else if(Boolean.TRUE.equals(healthCheckResult)){
                // 心跳正常，服务器状态视为正常
                serviceIsAvailable(true);
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
        // 设置JVM的启动时间
        jvmInfo.setJvmStartTime(runtimeMXBean.getStartTime());
        return jvmInfo;
    }

    public void getRuntimeJVMInfo(){
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        // jvm已经运行的时间
        long uptime = runtimeMXBean.getUptime();
    }







}
