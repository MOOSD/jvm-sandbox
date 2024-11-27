package com.sandbox.module.dynamic;

import cn.newgrand.ck.executor.DataProcessor;
import cn.newgrand.ck.reporter.DataReporter;
import cn.newgrand.ck.reporter.LogDataReporter;
import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.LoadCompleted;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.AgentInfo;
import com.alibaba.jvm.sandbox.api.resource.ConfigInfo;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sandbox.module.Context.RequestContext;
import com.sandbox.module.node.MethodInfo;
import com.sandbox.module.node.MethodTree;
import com.sandbox.module.processor.CoverageDataConsumer;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.*;

@MetaInfServices(Module.class)
@Information(id = "method-info", version = "0.0.1", author = "hts")
public class MethodInfoModule implements Module, LoadCompleted {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Resource
    private ConfigInfo configInfo;

    @Resource
    private AgentInfo agentInfo;

    private DataProcessor<MethodTree> dataProcessor;

    @Override
    public void loadCompleted() {
        initModule();

        new EventWatchBuilder(moduleEventWatcher, EventWatchBuilder.PatternType.REGEX)
                .onClass(buildClassPattern())
                .includeSubClasses()
                .onAnyBehavior()
                .onWatching()
                .withCall()
                .onWatch(new AdviceListener() {

                    @Override
                    protected void before(Advice advice) throws Throwable {
                        final MethodTree methodTree;
                        if (advice.isProcessTop()) {
                            methodTree = new MethodTree(getMethodInfo(advice));
                            advice.attach(methodTree);
                        } else {
                            methodTree = advice.getProcessTop().attachment();
                            methodTree.begin(getMethodInfo(advice));
                        }
                    }


                    public MethodInfo getMethodInfo(final Advice advice) {
                        MethodInfo methodInfo = new MethodInfo();
                        String methodName = advice.getBehavior().getName();
                        String className = Objects.nonNull(advice.getTarget()) ? advice.getTarget().getClass().getName() : "null";
                        methodInfo.setClassName(className);
                        methodInfo.setMethodName(methodName);
                        RequestContext requestTtl = TraceIdModule.getRequestTtl();
                        if (Objects.nonNull(requestTtl)) {
                            methodInfo.setTraceId(requestTtl.getTraceId());
                            methodInfo.setSpanId(requestTtl.getSpanId());
                        }
                        return methodInfo;
                    }

                    @Override
                    protected void afterReturning(Advice advice) throws Throwable {
                        final MethodTree methodTree = advice.getProcessTop().attachment();
                        MethodInfo methodInfo = methodTree.getCurrentData();
                        if (advice.getReturnObj() != null) {
                            methodInfo.setData(advice.getReturnObj().toString());
                        }
                        methodTree.setCurrentData(methodInfo);
                        methodTree.end();
                        if(advice.isProcessTop()&&Objects.nonNull(TraceIdModule.getRequestTtl())){
                            dataProcessor.add(advice.getProcessTop().attachment());
                        }
                    }

                    @Override
                    protected void afterThrowing(Advice advice) throws Throwable {
                        final MethodTree methodTree = advice.getProcessTop().attachment();
                        MethodInfo methodInfo = methodTree.getCurrentData();
                        methodInfo.setLog(advice.getThrowable().toString());
                        methodTree.begin(methodInfo).end();
                        methodTree.end();
                        if(advice.isProcessTop()&&Objects.nonNull(TraceIdModule.getRequestTtl())){
                            dataProcessor.add(advice.getProcessTop().attachment());
                        }
                    }
                });
        if(agentInfo.hKServiceIsAvailable()){
            dataProcessor.enable();
        }
    }

    @Override
    public void hkServerStateChange(boolean available) {
        logger.info("鹰眼服务端状态发生变化：{}",available);
        if (available){
            dataProcessor.enable();
        }else {
            dataProcessor.disable();
        }
    }

    private void initModule() {
        logger.info("动态调用链路模块加载完成！");
        // 创建数据发送器
        DataReporter dataReporter = new LogDataReporter(configInfo);
        // 创建消费者
        CoverageDataConsumer coverageDataConsumer = new CoverageDataConsumer(configInfo, dataReporter, agentInfo);

        // 创建数据消费者
        this.dataProcessor = new DataProcessor<>(1, 100, coverageDataConsumer);
    }


    //根据实际情况 构建匹配类的正则表达式
    private String buildClassPattern() {
        String moduleTracePattern = configInfo.getModuleTracePattern();
        if (Objects.isNull(moduleTracePattern) || moduleTracePattern.isEmpty()){
            logger.info("未指定项目所用类通配符，使用默认通配符");
            return "^cn\\.newgrand\\.pm\\.pcm\\.contract.*";
        }
        logger.info("项目所用类通配符:{}",moduleTracePattern);
        return moduleTracePattern;
//        return "^cn\\.newgrand\\.ck\\.(controller|service|util|mapper).*";
    }
}
