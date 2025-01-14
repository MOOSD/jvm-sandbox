package com.sandbox.module.dynamic;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.LoadCompleted;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

@MetaInfServices(Module.class)
@Information(id = "jdbc-info", version = "0.0.1", author = "qq")
public class JDBCInfoModule implements Module, LoadCompleted {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Override
    public void loadCompleted() {
        // 设置插件监听器
//        new EventWatchBuilder(moduleEventWatcher, EventWatchBuilder.PatternType.REGEX)
//                .onClass(buildPluginPattern())
//                .includeSubClasses()
//                .includeBootstrap()
//                .onBehavior("(exec.*|set.*|prepareStatement)")
//                .onWatching()
//                .withCall()
//                .onWatch(new AdviceListener() {
//                    @Override
//                    protected void before(Advice advice) throws Throwable {
//                        if (Objects.nonNull(TraceIdModule.getRequestTtl())) {
//                            logger.info("调用方法：{}", advice.getBehavior().getName());
//                            if(Proxy.isProxyClass(advice.getBehavior().getDeclaringClass())) {
//                                logger.info("调用类：{}", Arrays.stream(advice.getBehavior().getDeclaringClass().getInterfaces()).map(Class::getName).collect(Collectors.joining(",")));
//                            }
//                            else{
//                                logger.info("调用类：{}", advice.getBehavior().getDeclaringClass().getName());
//                            }
//                            // 调用参数
//                            Object[] args = advice.getParameterArray();
//                            logger.info("调用参数数量：{}", args.length);
//                            ObjectMapper objectMapper = new ObjectMapper();
//                            if (args.length > 0) {
//                                logger.info("调用参数：{}", objectMapper.writeValueAsString(args));
//                            }
//                            else {
//                                logger.info("调用参数类型：{}", objectMapper.writeValueAsString(advice.getBehavior()));
//                            }
//                        }
//                    }
//
//                    @Override
//                    protected void afterReturning(Advice advice) throws Throwable {
//                        if (Objects.nonNull(TraceIdModule.getRequestTtl())) {
//                            logger.info("返回值{}" , advice.getReturnObj());
//                        }
//                    }
//                });
    }

    private String buildPluginPattern(){
        return "java.sql.(Statement|Connection|PreparedStatement)";
    }
}
