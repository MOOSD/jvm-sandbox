package com.sandbox.module.dynamic;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.LoadCompleted;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.alibaba.jvm.sandbox.api.tools.JSON;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.sandbox.module.domain.RequestContext;
import com.sandbox.module.constant.LinkConstant;
import com.sandbox.module.node.MethodInfo;
import com.sandbox.module.node.MethodTree;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * TraceIdModule 通过拦截不同类的方法来为每个请求生成和维护 traceId 和相关上下文。
 * 它通过 Sandbox 框架，动态地修改请求和响应的头部，实现全链路追踪。
 */
@MetaInfServices(Module.class)
@Information(id = "trace-id", version = "0.0.1", author = "hts")
public class TraceIdModule implements Module, LoadCompleted {

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // 保存请求上下文到当前线程
    final static TransmittableThreadLocal<RequestContext> requestTtl = new TransmittableThreadLocal<>();

    // 获取请求上下文
    public static RequestContext getRequestTtl() {
        return requestTtl.get();
    }


    @Override
    public void loadCompleted() {
        // 监听 Spring WebFlux 中的 DispatcherHandler，处理 WebFlux 请求
        new EventWatchBuilder(moduleEventWatcher)
                .onClass("org.springframework.web.reactive.DispatcherHandler")
                .includeSubClasses()
                .onBehavior("handle")
                .onWatching()
                .withLine()
                .onWatch(new AdviceListener() {

                    @Override
                    protected void before(Advice advice) throws Throwable {
                        // 获取 ServerWebExchange 对象
                        Object serverWebExchange = advice.getParameterArray()[0];
                        // 通过反射获取 getRequest 方法并获取请求对象
                        Method getRequest = getMethod(serverWebExchange, "getRequest");
                        Object serverHttpRequest = getRequest.invoke(serverWebExchange);
                        Class<?> serverHttpRequestClazz = serverHttpRequest.getClass();
                        // 获取请求头中的 traceId、spanId 和创建时间
                        Method getHeaders = getMethod(serverHttpRequest, "getHeaders");
                        Object headers = getHeaders.invoke(serverHttpRequest);

                        Method get = headers.getClass().getMethod("get", Object.class);
                        get.setAccessible(true);

                        // 获取 traceId、spanId 和请求创建时间
                        Object headerTraceId = get.invoke(headers, LinkConstant.TRACE_ID);
                        Object headerSpan = get.invoke(headers, LinkConstant.SPAN_ID);
                        Object headerTime = get.invoke(headers, LinkConstant.REQUEST_CREATE_TIME);

                        // 获取请求 URI
                        String requestURI = (String) Objects.requireNonNull(getMethod(headers, "getRequestURI")).invoke(headers);
                        String requestUrl = (String) Objects.requireNonNull(getMethod(serverHttpRequest, "getURI")).invoke(serverHttpRequest);
                        String requestMethod = (String) Objects.requireNonNull(getMethod(serverHttpRequest, "getMethodValue")).invoke(serverHttpRequest);
                        // 如果 traceId 不存在，则生成一个新的 traceId 并设置到请求头
                        if (Objects.isNull(headerTraceId)) {
                            String uuid = UUID.randomUUID().toString();
                            RequestContext requestContext = new RequestContext(uuid, null, "(String) agent", requestUrl,requestMethod, System.currentTimeMillis());
                            requestTtl.set(requestContext);
                            // 修改请求头，将 traceId 添加到请求头中
                            Method mutate = serverHttpRequestClazz.getMethod("mutate");
                            Object builder = mutate.invoke(serverHttpRequest);
                            Method header = builder.getClass().getMethod("header", String.class, String[].class);
                            header.setAccessible(true);
                            header.invoke(builder, LinkConstant.TRACE_ID, new String[]{uuid});
                            logger.info("Generated new traceId: {}", uuid);  // 记录生成的 traceId
                        } else {
                            // 如果 traceId 存在，设置当前线程的 traceId 和其他信息
                            RequestContext requestContext = new RequestContext((String) headerTraceId, (String) headerSpan, (String) getHeader(headers, "User-Agent"), requestURI,requestMethod, (Long) headerTime);
                            requestTtl.set(requestContext);
                            logger.info("Using existing traceId: {}", headerTraceId);  // 记录使用的 traceId
                        }
                    }

                    @Override
                    protected void afterReturning(Advice advice) {
                        if (!advice.isProcessTop()) {
                            return;
                        }
                        // 清理线程局部变量，防止内存泄漏
                        requestTtl.remove();
                    }

                    @Override
                    protected void afterThrowing(Advice advice) {
                        if (!advice.isProcessTop()) {
                            return;
                        }
                        requestTtl.remove();
                    }
                });

        // 监听 Spring MVC 中的 DispatcherServlet，处理传统请求
        new EventWatchBuilder(moduleEventWatcher)
                .onClass("org.springframework.web.servlet.DispatcherServlet")
                .includeSubClasses()
                .onBehavior("doService")
                .withParameterTypes("javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse")
                .onWatch(new AdviceListener() {

                    @Override
                    protected void before(Advice advice) throws Throwable {
                        Object requestObj = advice.getParameterArray()[0];
                        Object headerTraceId = getHeader(requestObj, LinkConstant.TRACE_ID);
                        Object headerSpanId = getHeader(requestObj, LinkConstant.SPAN_ID);
                        Object headerCreate = getHeader(requestObj, LinkConstant.REQUEST_CREATE_TIME);

                        logger.info("traceId: {} is String {};\ncreate:{} is Long {}", headerTraceId,headerTraceId instanceof String,headerCreate,headerCreate instanceof Long);  // 记录使用的 traceId
                        String requestURI = (String) Objects.requireNonNull(getMethod(requestObj, "getRequestURI")).invoke(requestObj);
                        String requestMethod = (String) Objects.requireNonNull(getMethod(requestObj, "getMethod")).invoke(requestObj);
                        if (headerTraceId != null) {
                            RequestContext requestContext = new RequestContext((String) headerTraceId, (String) headerSpanId, (String) getHeader(requestObj, "User-Agent"), requestURI,requestMethod,(Long) headerCreate);
                            requestTtl.set(requestContext);
                        }
                        else  {
                            String uuid = UUID.randomUUID().toString();
                            RequestContext requestContext = new RequestContext(uuid, null, (String) getHeader(requestObj, "User-Agent"), requestURI,requestMethod,System.currentTimeMillis());
                            requestTtl.set(requestContext);
                        }
                    }

                    @Override
                    protected void afterReturning(Advice advice) {
                        if (!advice.isProcessTop()) {
                            return;
                        }
                        requestTtl.remove();
                    }

                    @Override
                    protected void afterThrowing(Advice advice) {
                        if (!advice.isProcessTop()) {
                            return;
                        }
                        requestTtl.remove();
                    }
                });

        // 监听 Feign 请求，用于添加 traceId 和 spanId 到请求头
        new EventWatchBuilder(moduleEventWatcher)
                .onClass("feign.SynchronousMethodHandler")
                .includeBootstrap()
                .includeSubClasses()
                .onBehavior("executeAndDecode")
                .onWatching()
                .onWatch(new AdviceListener() {

                    @Override
                    protected void before(Advice advice) throws Throwable {
                        Object o = advice.getParameterArray()[0];
                        RequestContext r = requestTtl.get();
                        Integer sort = r.getSort();
                        String spanId = requestTtl.get().getSpanId() + "->" + sort;
                        r.addSortRpc(sort);
                        header(o, LinkConstant.TRACE_ID, r.getSpanId());
                        header(o, LinkConstant.SPAN_ID, spanId);
                        header(o, LinkConstant.REQUEST_CREATE_TIME, r.getRequestCreateTime().toString());
                    }
                });
    }

    // 设置请求头的通用方法
    private void header(Object obj, String name, String... values) {
        try {
            Method header = obj.getClass().getMethod("header", String.class, String[].class);
            header.invoke(obj, name, values);
        } catch (Exception ignored) {
        }
    }

    // 获取请求头
    private Object getHeader(Object obj, String headerName) {
        try {
            Method setAttributeMethod = obj.getClass().getMethod("getHeader", String.class);
            return setAttributeMethod.invoke(obj, headerName);
        } catch (Exception e) {
            return null;
        }
    }

    // 获取方法的通用方法
    private Method getMethod(Object obj, String methodName) {
        try {
            return obj.getClass().getMethod(methodName);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
