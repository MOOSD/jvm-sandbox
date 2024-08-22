package com.sandbox.module.dynamic;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.LoadCompleted;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sandbox.module.Context.RequestContext;
import com.sandbox.module.node.MethodInfo;
import com.sandbox.module.node.MethodTree;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.util.*;

@MetaInfServices(Module.class)
@Information(id = "method-info", version = "0.0.1", author = "hts")
public class MethodInfoModule implements Module, LoadCompleted {

    private final Logger logger = LoggerFactory.getLogger("METHOD-INFO");


    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    final Map<String, Set<String>> map = new HashMap<>();

    final List<String> methods = new ArrayList<>();

    @Override
    public void loadCompleted() {

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

                        Class<?> clazz = Class.forName("com.example.ServiceProperties");
                        Object instance = clazz.getDeclaredConstructor().newInstance();

                        Field field = clazz.getDeclaredField("name");
                        field.setAccessible(true);
                        String serviceName = (String) field.get(instance);

                        logger.info("serviceName:[{}]", serviceName);
                    }


                    public MethodInfo getMethodInfo(final Advice advice){
                        MethodInfo methodInfo = new MethodInfo();
                        methodInfo.setClassName(advice.getBehavior().getDeclaringClass().getName());
                        methodInfo.setMethodName(advice.getBehavior().getName());
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
                        methodTree.end();
                        sendMessage(advice);
                    }

                    @Override
                    protected void afterThrowing(Advice advice) throws Throwable {
                        final MethodTree methodTree = advice.getProcessTop().attachment();


                        MethodInfo methodInfo = new MethodInfo();
                        methodInfo.setClassName(advice.getThrowable().getClass().getName());
                        RequestContext requestTtl = TraceIdModule.getRequestTtl();
                        if(Objects.nonNull(requestTtl)){
                            methodInfo.setTraceId(requestTtl.getTraceId());
                            methodInfo.setSpanId(requestTtl.getSpanId());
                        }
                        methodTree.begin(methodInfo).end();
                        methodTree.end();
                        sendMessage(advice);
                    }

                    @Override
                    protected void beforeCall(final Advice advice,
                                              final int callLineNum,
                                              final String callJavaClassName,
                                              final String callJavaMethodName,
                                              final String callJavaMethodDesc) {
                        final MethodTree methodTree = advice.getProcessTop().attachment();
                        MethodInfo methodInfo = new MethodInfo();
                        methodInfo.setClassName(callJavaClassName);
                        methodInfo.setMethodName(callJavaMethodName);
                        RequestContext requestTtl = TraceIdModule.getRequestTtl();
                        if(Objects.nonNull(requestTtl)){
                            methodInfo.setTraceId(requestTtl.getTraceId());
                            methodInfo.setSpanId(requestTtl.getSpanId());
                        }

                        methodTree.begin(methodInfo);
                    }

                    @Override
                    protected void afterCallReturning(final Advice advice,
                                                      final int callLineNum,
                                                      final String callJavaClassName,
                                                      final String callJavaMethodName,
                                                      final String callJavaMethodDesc) {
                        final MethodTree methodTree = advice.getProcessTop().attachment();
                        methodTree.end();
                    }

                    @Override
                    protected void afterCallThrowing(final Advice advice,
                                                     final int callLineNum,
                                                     final String callJavaClassName,
                                                     final String callJavaMethodName,
                                                     final String callJavaMethodDesc,
                                                     final String callThrowJavaClassName) {
                        final MethodTree methodTree = advice.getProcessTop().attachment();

                        MethodInfo methodInfo = new MethodInfo();
                        methodInfo.setClassName(advice.getThrowable().getClass().getName());
                        RequestContext requestTtl = TraceIdModule.getRequestTtl();
                        if(Objects.nonNull(requestTtl)){
                            methodInfo.setTraceId(requestTtl.getTraceId());
                            methodInfo.setSpanId(requestTtl.getSpanId());
                        }
                        methodTree.set(methodInfo).end();
                    }

                });
    }



    //根据实际情况 构建匹配类的正则表达式
    private String buildClassPattern() {
//        return "^cn\\.newgrand\\.ck.*";
        return "^cn\\.newgrand\\.ck(?!\\.(entity|log|config)).*";
    }


    private void sendMessage(Advice advice){
        if(advice.isProcessTop()){
            final MethodTree methodTree = advice.getProcessTop().attachment();
//            System.out.println("methodTree.rendering() = " + methodTree.rendering());

            String json = null;
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                json = objectMapper.writeValueAsString(methodTree.convertToDTO(methodTree.getCurrent()));
                System.out.println("json = " + json);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
