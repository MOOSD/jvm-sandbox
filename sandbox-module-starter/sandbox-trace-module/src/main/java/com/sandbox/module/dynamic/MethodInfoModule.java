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
import com.sandbox.module.Context.RequestContext;
import com.sandbox.module.node.MethodInfo;
import com.sandbox.module.node.MethodTree;
import com.sandbox.module.processor.TraceDataConsumer;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.lang.annotation.Annotation;
import java.util.*;

@MetaInfServices(Module.class)
@Information(id = "method-info", version = "0.0.1", author = "hts")
public class MethodInfoModule implements Module, LoadCompleted {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final HashSet<String> annotationSet = new HashSet<>();

    private final String[] defaultAnnotation = {"PostMapping", "GetMapping", "RequestMapping","PutMapping","DeleteMapping"};

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
                        if(Objects.nonNull(TraceIdModule.getRequestTtl())){
                            MethodInfo info = getMethodInfo(advice);
                            final MethodTree methodTree;
                            if (advice.isProcessTop()) {
                                methodTree = new MethodTree(info);
                                RequestContext reqTtl = TraceIdModule.getRequestTtl();
                                methodTree.setTraceId(reqTtl.getTraceId());
                                methodTree.setSpanId(reqTtl.getSpanId());
                                methodTree.setRequestUri(reqTtl.getRequestUrl());
                                advice.attach(methodTree);
                            } else {
                                methodTree = advice.getProcessTop().attachment();
                                methodTree.begin(info);
                            }
                            if(!methodTree.getSend()){
                                methodTree.setBegin(true);
                                methodTree.setSend(true);
                            }
                        }
                    }


                    public MethodInfo getMethodInfo(final Advice advice) {
                        MethodInfo methodInfo = new MethodInfo();
                        String methodName = advice.getBehavior().getName();
                        String className = Objects.nonNull(advice.getTarget()) ? advice.getTarget().getClass().getName() : Objects.nonNull(advice.getBehavior()) ? advice.getBehavior().getDeclaringClass().getName() : null;
                        methodInfo.setClassName(className);
                        methodInfo.setMethodName(methodName);
                            //获取注解
                        if ( advice.getBehavior() != null && advice.getBehavior().getAnnotations() != null && advice.getBehavior().getAnnotations().length > 0) {
                            Annotation[] annotations = advice.getBehavior().getAnnotations();
                            String[] annotationNames = new String[annotations.length];
                            boolean isSend = false;
                            for (int i = 0; i < annotations.length; i++) {
                                annotationNames[i] = annotations[i].annotationType().getSimpleName();
                                if(annotationSet.contains(annotationNames[i])){
                                    isSend = true;
                                }
                            }
                            methodInfo.setSend(isSend);
                            methodInfo.setAnnotations(annotationNames);
                            if(advice.getParameterArray() != null && advice.getParameterArray().length > 0){
                                methodInfo.setParams(Arrays.stream(advice.getParameterArray())
                                        .map(String::valueOf)  // 将每个元素转为 String
                                        .toArray(String[]::new));
                            }
                        }
                        return methodInfo;
                    }

                    @Override
                    protected void afterReturning(Advice advice) throws Throwable {
                        if(Objects.nonNull(TraceIdModule.getRequestTtl())){
                            final MethodTree methodTree = advice.getProcessTop().attachment();
                            if(methodTree.getBegin() && methodTree.getCurrentData().getSend()){
                                methodTree.setSend(false);
                                dataProcessor.add(advice.getProcessTop().attachment());
                            }
                            methodTree.end();
                        }
                    }

                    @Override
                    protected void afterThrowing(Advice advice) throws Throwable {
                        if(Objects.nonNull(TraceIdModule.getRequestTtl())){
                            final MethodTree methodTree = advice.getProcessTop().attachment();
                            if(Objects.nonNull(advice.getThrowable())){
                                MethodInfo methodInfo = new MethodInfo();
                                methodInfo.setLog(advice.getThrowable().getMessage());
                                methodTree.addBaseInfo(methodInfo);
                            }
                            if(methodTree.getBegin() && methodTree.getCurrentData().getSend()){
                                methodTree.setSend(false);
                                dataProcessor.add(advice.getProcessTop().attachment());
                            }
                            methodTree.end();
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
        TraceDataConsumer coverageDataConsumer = new TraceDataConsumer(configInfo, dataReporter, agentInfo);
        annotationSet.addAll(Arrays.asList(defaultAnnotation));
        // 创建数据消费者
        this.dataProcessor = new DataProcessor<>(5, 1000, coverageDataConsumer);
    }


    //根据实际情况 构建匹配类的正则表达式
    private String buildClassPattern() {
        String moduleTracePattern = configInfo.getModuleTracePattern();
        if (Objects.isNull(moduleTracePattern) || moduleTracePattern.isEmpty()){
            logger.info("未指定项目所用类通配符，使用默认通配符");
            return "^cn\\.newgrand.*";
        }
        logger.info("项目所用类通配符:{}",moduleTracePattern);
        return moduleTracePattern;
//        return "^cn\\.newgrand\\.ck\\.(controller|service|util|mapper).*";
    }
}
