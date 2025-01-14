package com.sandbox.module.dynamic;

import cn.newgrand.ck.executor.DataProcessor;
import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.LoadCompleted;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.AgentInfo;
import com.alibaba.jvm.sandbox.api.resource.ConfigInfo;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.alibaba.jvm.sandbox.api.tools.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sandbox.module.domain.RequestContext;
import com.sandbox.module.node.MethodInfo;
import com.sandbox.module.node.MethodTree;
import com.sandbox.module.node.TraceBaseInfo;
import com.sandbox.module.processor.TraceDataConsumer;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * 方法信息模块，用于动态监控方法的调用链路，收集调用信息并进行处理。
 * @author qq
 */
@MetaInfServices(Module.class)
@Information(id = "method-info", version = "0.0.1", author = "hts")
public class MethodInfoModule implements Module, LoadCompleted {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final HashSet<String> annotationSet = new HashSet<>();

    private final String[] defaultAnnotation = {"PostMapping", "GetMapping", "RequestMapping", "PutMapping", "DeleteMapping"};

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Resource
    private ConfigInfo configInfo;

    @Resource
    private AgentInfo agentInfo;

    private DataProcessor<MethodTree> dataProcessor;

    private static final Integer SORT_SIZE = 5000;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 模块加载完成后的初始化工作
     */
    @Override
    public void loadCompleted() {
        initModule();
        //设置基础方法监听器
        new EventWatchBuilder(moduleEventWatcher, EventWatchBuilder.PatternType.REGEX)
                .onClass(buildClassPattern())
                .includeSubClasses()
                .onAnyBehavior()
                .onWatching()
                .withCall()
                .onWatch(new AdviceListener() {
                    @Override
                    protected void before(Advice advice) throws Throwable {
                        // 获取TraceId，如果存在则处理方法信息
                        if (Objects.nonNull(TraceIdModule.getRequestTtl())) {
                            final MethodTree methodTree;
                            MethodInfo info = initInfo(advice);
                            if (advice.isProcessTop()) {
                                // 如果是最顶层方法，创建新的方法树
                                methodTree = new MethodTree(getMethodInfo(advice, info));
                                initTree(methodTree , objectMapper.writeValueAsString(advice.getParameterArray()));
                                advice.attach(methodTree); // 将方法树附加到Advice上
                            }else {
                                // 如果是嵌套方法，继续处理
                                methodTree = advice.getProcessTop().attachment();
                                methodTree.begin(getMethodInfo(advice, info));
                            }
                            if (!methodTree.getSend()) {
                                methodTree.setBegin(true);
                                methodTree.setSend(true);
                            }
                            if(TraceIdModule.getRequestTtl().getSort() > SORT_SIZE){
                                // 当前调用链路节点数超过5000，请修正插桩点，减小节点数据
                                logger.warn("当前调用链路节点数超过5000，请修正插桩点，减小节点数据");
                                logger.warn("当前调用链路Api：{}", TraceIdModule.getRequestTtl().getRequestUrl());
                                TraceIdModule.removeRequestTtl();
                                advice.getProcessTop().attach(null);
                            }
                        }
                    }

                    private MethodInfo initInfo(Advice advice) {
                        MethodInfo methodInfo = new MethodInfo();
                        String methodName = advice.getBehavior().getName();
                        String className = null;
                        methodInfo.setParams(
                                Arrays.stream(advice.getBehavior().getParameterTypes())
                                        .map(Class::getName) // 获取参数的全限定类名
                                        .toArray(String[]::new)
                        );
                        methodInfo.setReturnType(advice.getBehavior().getReturnType().getName());
                        if( advice.getBehavior() != null){
                            className = advice.getBehavior().getDeclaringClass().getName();
                            methodInfo.setBehavior(className);
                        }
                        if(advice.getTarget() != null){
                            if(Proxy.isProxyClass(advice.getTarget().getClass())){
                                className = advice.getTarget().getClass().getInterfaces()[0].getName();
                                methodInfo.setTarget(className);
                            }
                        }
                        methodInfo.setClassName(className);
                        methodInfo.setMethodName(methodName);
                        return methodInfo;
                    }

                    /**
                     * 初始化方法树的属性
                     */
                    public void initTree(MethodTree methodTree , String json) {
                        RequestContext reqTtl = TraceIdModule.getRequestTtl();
                        Long requestCreateTime = reqTtl.getRequestCreateTime();
                        methodTree.setTraceId(reqTtl.getTraceId());
                        methodTree.setIntoData(json);
                        methodTree.setSpanId(reqTtl.getSpanId());
                        methodTree.setRequestUri(reqTtl.getRequestUrl());
                        methodTree.setRequestMethod(reqTtl.getRequestMethod());
                        methodTree.setRequestCreateTime(requestCreateTime);
                    }

                    /**
                     * 获取方法信息，包括方法名、类名、注解信息和方法参数
                     */
                    public MethodInfo getMethodInfo(final Advice advice , MethodInfo methodInfo) throws NoSuchMethodException {
                        // 获取方法的注解信息
                        if (advice.getBehavior() != null && advice.getBehavior().getAnnotations() != null && advice.getBehavior().getAnnotations().length > 0) {
                            Annotation[] annotations = advice.getBehavior().getAnnotations();
                            String[] annotationNames = new String[annotations.length];
                            boolean isSend = false;
                            for (int i = 0; i < annotations.length; i++) {
                                annotationNames[i] = annotations[i].annotationType().getSimpleName();
                                if (annotationSet.contains(annotationNames[i])) {
                                    Class<?> clazz = advice.getBehavior().getDeclaringClass();
                                    Method method = clazz.getMethod(methodInfo.getMethodName(),advice.getBehavior().getParameterTypes());
                                    TraceIdModule.getRequestTtl().setClazzApiPath(getClassMappingValue(clazz));
                                    TraceIdModule.getRequestTtl().setMethodApiPath(getMethodMappingValue(method));
                                    isSend = true; // 如果包含指定注解，则设置为发送
                                }
                            }
                            methodInfo.setSend(isSend);
                            methodInfo.setAnnotations(annotationNames);
                        }
                        return methodInfo;
                    }

                    @Override
                    protected void beforeCall(Advice advice,
                                              int callLineNum,
                                              String callJavaClassName, String callJavaMethodName, String callJavaMethodDesc) {
                        if (Objects.nonNull(TraceIdModule.getRequestTtl())) {
                            final MethodTree methodTree = advice.getProcessTop().attachment() != null ? advice.getProcessTop().attachment() : null;
                            if (methodTree != null) {
                                // 添加被调用方法的信息
                                methodTree.addMethodCell(callLineNum + " : " + callJavaClassName + " : " + callJavaMethodName);
                            }
                        }
                    }

                    @Override
                    protected void afterReturning(Advice advice) throws Throwable {
                        if (Objects.nonNull(TraceIdModule.getRequestTtl())) {
                            final MethodTree methodTree = advice.getProcessTop().attachment();
                            if (methodTree.getBegin() && methodTree.getCurrentData().getSend()) {
                                methodTree.setSortRpc(TraceIdModule.getRequestTtl().getSortRpc());
                                methodTree.setSend(true);
                                RequestContext rtl = TraceIdModule.getRequestTtl();
                                if(rtl.getClazzApiPath() != null){
                                    methodTree.setClazzApiPath(rtl.getClazzApiPath());
                                    methodTree.setMethodApiPath(rtl.getMethodApiPath());
                                }
                                methodTree.setOutData(objectMapper.writeValueAsString(advice.getReturnObj()));
                                // 添加数据到处理器
                                MethodTree data = methodTree;
                                TraceBaseInfo traceBaseInfo = new TraceBaseInfo();
                                traceBaseInfo.setAgentId(agentInfo.getInstanceId());
                                traceBaseInfo.setRequestCreateTime(data.getRequestCreateTime());
                                traceBaseInfo.setTraceId(data.getTraceId());
                                traceBaseInfo.setOutData(data.getOutData());
                                traceBaseInfo.setIntoData(data.getIntoData());
                                traceBaseInfo.setErrorData(data.getErrorData());
                                traceBaseInfo.setSpanId(data.getSpanId());
                                traceBaseInfo.setRequestUrl(data.getRequestUri());
                                traceBaseInfo.setSort(data.getSort());
                                traceBaseInfo.setSortRpc(data.getSortRpc());
                                traceBaseInfo.setRequestMethod(data.getRequestMethod());
                                traceBaseInfo.setClazzApiPath(data.getClazzApiPath());
                                traceBaseInfo.setMethodApiPath(data.getMethodApiPath());
                                traceBaseInfo.setSimpleTree(data.convertToDTO(data.getCurrent(),data.getSort()));
                                ObjectMapper objectMapper = new ObjectMapper();
                                String json = objectMapper.writeValueAsString(traceBaseInfo);
                                logger.info("traceId: {}, traceBaseInfo: {}", data.getTraceId(), json);
                                dataProcessor.add(advice.getProcessTop().attachment());
                            }
                            methodTree.end();
                        }
                    }

                    @Override
                    protected void afterThrowing(Advice advice) throws Throwable {
                        if (Objects.nonNull(TraceIdModule.getRequestTtl())) {
                            final MethodTree methodTree = advice.getProcessTop().attachment();
                            if (Objects.nonNull(advice.getThrowable())) {
                                MethodInfo methodInfo = new MethodInfo();
                                methodInfo.setLog(advice.getThrowable().getMessage());
                                methodTree.addBaseInfo(methodInfo);
                            }
                            if (methodTree.getBegin() && methodTree.getCurrentData().getSend()) {
                                methodTree.setSortRpc(TraceIdModule.getRequestTtl().getSortRpc());
                                methodTree.setSend(true);
                                methodTree.setErrorData(JSON.toJSONString(advice.getThrowable()));
                                dataProcessor.add(advice.getProcessTop().attachment());
                            }
                            methodTree.end();
                        }
                    }
                });

        // 启用数据处理器
        if (agentInfo.hKServiceIsAvailable()) {
            dataProcessor.enable();
        }
    }

    @Override
    public void hkServerStateChange(boolean available) {
        logger.info("鹰眼服务端状态发生变化：{}", available);
        if (available) {
            dataProcessor.enable();
        } else {
            dataProcessor.disable();
        }
    }

    /**
     * 初始化模块，设置数据发送器、消费者等
     */
    private void initModule() {
        logger.info("动态调用链路模块加载完成！");
        // 创建消费者
        TraceDataConsumer coverageDataConsumer = new TraceDataConsumer(configInfo, agentInfo);
        annotationSet.addAll(Arrays.asList(defaultAnnotation));
        // 创建数据处理器
        this.dataProcessor = new DataProcessor<>(5, 1000, coverageDataConsumer);
    }

    /**
     * 根据配置构建匹配类的正则表达式
     */
    private String buildClassPattern() {
        String moduleTracePattern = configInfo.getModuleTracePattern();
        if (Objects.isNull(moduleTracePattern) || moduleTracePattern.isEmpty()) {
            logger.info("未指定项目所用类通配符，使用默认通配符");
            return "^cn\\.newgrand.*";
        }
        logger.info("项目所用类通配符: {}", moduleTracePattern);
        return moduleTracePattern;
    }

    private String buildMethodPattern() {
        String moduleTracePattern = configInfo.getModuleTracePattern();
        if(Objects.nonNull(moduleTracePattern) && moduleTracePattern.contains("!")) {
            return ".*";
        }
        else{
            logger.info("未指定不监控类，默认不插桩get,set,is开头方法");
            return "^(?!get|set|is).*";
        }
    }


    public String[] getClassMappingValue(Class<?> clazz) {
        // 遍历所有注解
        for (Annotation annotation : clazz.getAnnotations()) {
            String[] value = getMappingAnnotationValue(annotation);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * 获取方法上的 Mapping 注解的 value 值
     */
    public String[] getMethodMappingValue(Method method) {
        for (Annotation annotation : method.getAnnotations()) {
            String[] value = getMappingAnnotationValue(annotation);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * 通用提取注解 value 属性的方法
     */
    private String[] getMappingAnnotationValue(Annotation annotation) {
        // 根据注解的全限定类名进行判断
        String annotationName = annotation.annotationType().getName();
        if ("org.springframework.web.bind.annotation.RequestMapping".equals(annotationName) ||
                "org.springframework.web.bind.annotation.GetMapping".equals(annotationName) ||
                "org.springframework.web.bind.annotation.PostMapping".equals(annotationName) ||
                "org.springframework.web.bind.annotation.PutMapping".equals(annotationName) ||
                "org.springframework.web.bind.annotation.DeleteMapping".equals(annotationName)) {

            try {
                // 通过反射调用注解的 value 方法
                Method valueMethod = annotation.annotationType().getMethod("value");
                Method pathMethod = annotation.annotationType().getMethod("path");
                String[] values = (String[]) valueMethod.invoke(annotation);
                String[] paths = (String[]) pathMethod.invoke(annotation);
                return values.length > 0 ? values : paths.length>0? paths : null;
            } catch (Exception e) {
                // 捕获所有异常，避免出错
                e.printStackTrace();
            }
        }
        return null;
    }
}
