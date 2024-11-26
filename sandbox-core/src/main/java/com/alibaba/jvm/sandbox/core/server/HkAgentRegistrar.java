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
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 鹰眼的agent注册器
 */
public class HkAgentRegistrar {
    private final List<HKServerObserver> HKServerObservers = new LinkedList<>();
    Logger logger = LoggerFactory.getLogger(getClass());
    private final CoreConfigure configure;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    private boolean HKServerIsAvailable = false;

    public HkAgentRegistrar(CoreConfigure cfg) {
        this.configure = cfg;
    }

    /**
     * 添加服务器观察者
     * @param hkServerObserver
     */
    synchronized public void addHKServerObserver(HKServerObserver hkServerObserver){
        HKServerObservers.add(hkServerObserver);
    }

    /**
     * todo 线程安全问题
     */
    synchronized public void serviceIsAvailable(boolean available){
        if (HKServerIsAvailable == available){
            return;
        }
        HKServerIsAvailable = available;
        logger.info("服务器状态发生变化:{}, 通知所有观察者",available);
        // 通知所有观察者
        for (HKServerObserver hkServerObserver : HKServerObservers) {
            hkServerObserver.HKServerStateNotify(available);
        }
    }


    /**
     * 激活Register自律行为
     * 注册失败会排除异常
     */
    public void active() throws Exception {
        checkConfig();
        RegisterResponseVO registerVO = register();
        if (Objects.isNull(registerVO) || !Boolean.TRUE.equals(registerVO.getSuccess())){
            logger.error("注册失败:{}",registerVO);
            throw new RuntimeException("注册失败");
        }
        logger.info("agent注册成功:{}", registerVO.getSuccess());
        // 开启健康检查
        healthStatusCheck();
        // 注册关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(this::deregister));

    }

    /**
     * 注册接口
     */
    public RegisterResponseVO register() throws IOException {
        // 构建请求
        RegisterRequest registerRequest = getRegisterRequest();
        String url = HkUtils.getUrl(configure.getHkServerIp(), configure.getHkServerPort(), ApiPathConstant.REGISTER_URL);
        return HttpClientUtil.postByJson(url, registerRequest, RegisterResponseVO.class);
    }

    private RegisterRequest getRegisterRequest() {
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
        return registerRequest;
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
    private void healthStatusCheck(){

        Integer healthCheckCycle = configure.getHkHealthCheckCycle();
        if (0 == healthCheckCycle){
            return ;
        }
        String healthCheckUrl = HkUtils.getUrl(configure.getHkServerIp(), configure.getHkServerPort(), ApiPathConstant.HEALTH_CHECK_URL);
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            Exception healthCheckException = null;
            Boolean healthCheckResult = null;
            try {
                AgentStatusRequest agentStatusRequest = new AgentStatusRequest();
                agentStatusRequest.setTimeStamp(System.currentTimeMillis());
                agentStatusRequest.setInstanceId(configure.getInstanceId());
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
                try {
                    register();
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
        return jvmInfo;
    }

    public void getRuntimeJVMInfo(){
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        // jvm已经运行的时间
        long uptime = runtimeMXBean.getUptime();
    }







}
