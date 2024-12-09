package com.sandbox.module.dynamic;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.LoadCompleted;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.sandbox.module.Context.RequestContext;
import com.sandbox.module.constant.LinkConstant;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@MetaInfServices(Module.class)
@Information(id = "trace-id", version = "0.0.1", author = "hts")
public class TraceIdModule implements Module, LoadCompleted {
    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    final static TransmittableThreadLocal<Integer> sortTtl = new TransmittableThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return 0;
        }
    };
    public static Integer getSort(){
        return sortTtl.get();
    }
    public static void setSort(int sort){
        sortTtl.set(sort);
    }
    final static TransmittableThreadLocal<List<Integer>> sortList = new TransmittableThreadLocal<List<Integer>>(){
        @Override
        protected List<Integer> initialValue() {
            return new ArrayList<>();
        }
    };
    public static List<Integer> getSortList(){
        return sortList.get();
    }
    final static TransmittableThreadLocal<String> traceIdThreadLocal = new TransmittableThreadLocal<>();
    final static TransmittableThreadLocal<RequestContext> requestTtl = new TransmittableThreadLocal<>();
    public static String getThreadLocalValue(){
        return traceIdThreadLocal.get();
    }


    public static RequestContext getRequestTtl(){
        return requestTtl.get();
    }

    @Override
    public void loadCompleted() {

        new EventWatchBuilder(moduleEventWatcher)
                .onClass("org.springframework.web.reactive.DispatcherHandler")
                .includeSubClasses()
                .onBehavior("handle")
                .onWatching()
                .withLine()
                .onWatch(new AdviceListener() {


                    @Override
                    protected void before(Advice advice) throws Throwable {
                        Object serverWebExchange = advice.getParameterArray()[0];
                        Method getRequest = getMethod(serverWebExchange, "getRequest");
                        Object serverHttpRequest = getRequest.invoke(serverWebExchange);
                        Class<?> serverHttpRequestClazz = serverHttpRequest.getClass();
                        // 通过反射获取请求头中的traceId属性
                        Method getHeaders = getMethod(serverHttpRequest, "getHeaders");
                        Object headers = getHeaders.invoke(serverHttpRequest);

                        Method get = headers.getClass().getMethod("get", Object.class);

                        get.setAccessible(true);
                        Object headerList = get.invoke(headers, LinkConstant.TRACE_ID);
                        Object headerSpan = get.invoke(headers, LinkConstant.SPAN_ID);


                        Object userAgent = get.invoke(headers, "User-Agent");
                        String requestUrl = (String) Objects.requireNonNull(getMethod(serverHttpRequest, "getURI")).invoke(serverHttpRequest);

                        // 如果 taceId 不存在则生成唯一 traceId 并且将其放入到请求头中
                        if(Objects.isNull(headerList)){
                            String uuid = UUID.randomUUID().toString();
                            RequestContext requestContext = new RequestContext(uuid, null, "(String) agent" , requestUrl);
                            traceIdThreadLocal.set(uuid);
                            requestTtl.set(requestContext);
                            Method mutate = serverHttpRequestClazz.getMethod("mutate");
                            Object builder = mutate.invoke(serverHttpRequest);
                            Method header = builder.getClass().getMethod("header", String.class, String[].class);
                            header.setAccessible(true);
                            header.invoke(builder, LinkConstant.TRACE_ID, new String[]{uuid});
                        }
                    }

                    @Override
                    protected void afterReturning(Advice advice) {

                        if (!advice.isProcessTop()) {
                            return;
                        }

                        traceIdThreadLocal.remove();
                        requestTtl.remove();
                        sortTtl.remove();
                        sortList.remove();

                    }

                    @Override
                    protected void afterThrowing(Advice advice) {

                        if (!advice.isProcessTop()) {
                            return;
                        }
                        traceIdThreadLocal.remove();
                        requestTtl.remove();
                        sortTtl.remove();
                        sortList.remove();
                    }
                });


        new EventWatchBuilder(moduleEventWatcher)
                .onClass("org.springframework.web.servlet.DispatcherServlet")
                .includeSubClasses()
                .onBehavior("doService")
                .withParameterTypes(
                        "javax.servlet.http.HttpServletRequest",
                        "javax.servlet.http.HttpServletResponse"
                )
                .onWatch(new AdviceListener() {

                    @Override
                    protected void before(Advice advice) throws Throwable {
                        Object requestObj = advice.getParameterArray()[0];
                        Object headerTraceId = getHeader(requestObj, LinkConstant.TRACE_ID);
                        Object headerTraceIdByStr = getHeader(requestObj, "traceId");
                        Object headerSpanId = getHeader(requestObj, LinkConstant.SPAN_ID);
                        String requestURI = (String) Objects.requireNonNull(getMethod(requestObj, "getRequestURI")).invoke(requestObj);


                        if(headerTraceId != null){
                            traceIdThreadLocal.set((String) headerTraceId);
                            RequestContext requestContext = new RequestContext((String) headerTraceId, (String) headerSpanId, (String) getHeader(requestObj, "User-Agent"),requestURI);
                            requestTtl.set(requestContext);
                            setAttribute(requestObj, LinkConstant.TRACE_ID, getThreadLocalValue());
                        }

                        if(traceIdThreadLocal.get() == null){
                            String uuid = UUID.randomUUID().toString();
                            RequestContext requestContext = new RequestContext(uuid, null, (String) getHeader(requestObj, "User-Agent"),requestURI);
                            traceIdThreadLocal.set(uuid);
                            requestTtl.set(requestContext);
                            logger.info("Injected traceId: {} into request: {}", uuid, requestURI);
                        }

                    }

                    @Override
                    protected void afterReturning(Advice advice) {
                        if (!advice.isProcessTop()) {
                            return;
                        }
                        traceIdThreadLocal.remove();
                        requestTtl.remove();
                        sortTtl.remove();
                        sortList.remove();
                    }

                    @Override
                    protected void afterThrowing(Advice advice) {
                        if (!advice.isProcessTop()) {
                            return;
                        }
                        traceIdThreadLocal.remove();
                        requestTtl.remove();
                        sortTtl.remove();
                        sortList.remove();
                    }
                });


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
                        Integer sort = sortTtl.get();
                        String spanId = requestTtl.get().getSpanId() + "->" + sort;
                        sortList.get().add(sort);
                        logger.info("feign spanId:{}", spanId);
                        header(o, LinkConstant.TRACE_ID, traceIdThreadLocal.get());
                        header(o,LinkConstant.SPAN_ID, spanId);
                    }
                });
    }


    private void header(Object obj,String name, String... values) {
        try {
            Method header = obj.getClass().getMethod("header", String.class, String[].class);
            header.invoke(obj, name, values);
        } catch (Exception ignored) {
        }
    }

    private Object headers(Object obj) {
        try {
            Method getAttributeMethod = obj.getClass().getMethod("headers");
            return getAttributeMethod.invoke(obj);
        } catch (Exception e) {
            return null;
        }
    }


    private boolean isInstanceOf(Object obj, String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return clazz.isInstance(obj);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private Object getAttribute(Object obj, String attributeName) {
        try {
            Method getAttributeMethod = obj.getClass().getMethod("getAttribute", String.class);
            return getAttributeMethod.invoke(obj, attributeName);
        } catch (Exception e) {
            return null;
        }
    }

    private void setAttribute(Object obj, String attributeName, Object attributeValue) {
        try {
            Method setAttributeMethod = obj.getClass().getMethod("setAttribute", String.class, Object.class);
            setAttributeMethod.invoke(obj, attributeName, attributeValue);
        } catch (Exception e) {
            // Handle exception
        }
    }

    private Object getHeader(Object obj, String headerName) {
        try {
            Method setAttributeMethod = obj.getClass().getMethod("getHeader", String.class);
            return setAttributeMethod.invoke(obj, headerName);
        } catch (Exception e) {
            return null;
        }
    }

    private Method getMethod(Object obj, String methodName) {
        try {
            return obj.getClass().getMethod(methodName);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

}
