package com.sandbox.module.dynamic;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.LoadCompleted;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sandbox.module.Context.RequestContext;
import com.sandbox.module.node.MethodInfo;
import com.sandbox.module.node.MethodTree;
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

    @Override
    public void loadCompleted() {

        logger.info("动态调用链路模块加载完成！");

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
                        if(advice.isProcessTop()){
                            methodTree = new MethodTree(getMethodInfo(advice));
                            advice.attach(methodTree);
                        } else {
                            methodTree = advice.getProcessTop().attachment();
                            methodTree.begin(getMethodInfo(advice));
                        }
                    }


                    public MethodInfo getMethodInfo(final Advice advice){
                        MethodInfo methodInfo = new MethodInfo();
                        String methodName = advice.getBehavior().getName();
                        String className = Objects.nonNull(advice.getTarget()) ? advice.getTarget().getClass().getName() : "null";
                        methodInfo.setClassName(className);
                        methodInfo.setMethodName(methodName);
                        if(Objects.nonNull(advice.getParameterArray())){
                            Object[] objectArray = advice.getParameterArray();
                            String[] classNames = new String[objectArray.length];
                            for (int i = 0; i < objectArray.length; i++) {
                                classNames[i] = objectArray[i].getClass().getName();  // 获取类的完全限定名
                            }
                            methodInfo.setParams(classNames);
                        }
                        RequestContext requestTtl = TraceIdModule.getRequestTtl();
                        if(Objects.nonNull(requestTtl)){
                            methodInfo.setTraceId(requestTtl.getTraceId());
                            methodInfo.setSpanId(requestTtl.getSpanId());
                        }
                        return methodInfo;
                    }
                    @Override
                    protected void afterReturning(Advice advice) throws Throwable {
                        final MethodTree methodTree = advice.getProcessTop().attachment();
                        MethodInfo methodInfo = methodTree.getCurrentData();
                        if(advice.getReturnObj() != null){
                            methodInfo.setData(advice.getReturnObj().toString());
                        }
                        methodTree.setCurrentData(methodInfo);
                        methodTree.end();
                        sendMessage(advice);
                    }

                    @Override
                    protected void afterThrowing(Advice advice) throws Throwable {
                        final MethodTree methodTree = advice.getProcessTop().attachment();
                        MethodInfo methodInfo = methodTree.getCurrentData();
                        methodInfo.setLog(advice.getThrowable().toString());
                        methodTree.begin(methodInfo).end();
                        methodTree.end();
                        sendMessage(advice);
                    }
                });
    }



    //根据实际情况 构建匹配类的正则表达式
    private String buildClassPattern() {
        return "^cn\\.newgrand.*";
//        return "^cn\\.newgrand\\.ck\\.(controller|service|util|mapper).*";
    }


    private void sendMessage(Advice advice){
        if(advice.isProcessTop()){
            if(Objects.isNull(advice.getTarget()) || advice.getTarget().getClass().getName().contains("Controller")){
                final MethodTree methodTree = advice.getProcessTop().attachment();
                String json = null;
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                    json = objectMapper.writeValueAsString(methodTree.convertToDTO(methodTree.getCurrent()));
                    logger.info("方法[{}]调用链路:{} ", advice.getBehavior().getName(), json);
                } catch (JsonProcessingException e) {
                    logger.error("序列化方法调用链时发生异常: ", e);
                    throw new RuntimeException(e);
                }
            }else{
                try{
                    final MethodTree methodTree = advice.getProcessTop().attachment();
                    if(methodTree.isTop()){
                        logger.info("方法为根方法");
                    }else{
                        logger.info("方法{}调用链路:{} ", advice.getBehavior().getName(), methodTree.convertToDTO(methodTree.getCurrent()));
                    }
                } catch (Exception e){
                    logger.error("方法{}序列化报错{} ", advice.getBehavior().getName(), e.getMessage());
                }
            }
        }
    }
}
