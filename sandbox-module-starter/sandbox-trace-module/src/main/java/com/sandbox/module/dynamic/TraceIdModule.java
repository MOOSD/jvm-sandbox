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
import java.util.Objects;
import java.util.UUID;

@MetaInfServices(Module.class)
@Information(id = "trace-id", version = "0.0.1", author = "hts")
public class TraceIdModule implements Module, LoadCompleted {
    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

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
                        System.out.println("serverHttpRequest = " + serverHttpRequest);
                        Method getHeaders = getMethod(serverHttpRequest, "getHeaders");
                        Object headers = getHeaders.invoke(serverHttpRequest);
                        System.out.println("headers = " + headers);
                        Method get = headers.getClass().getMethod("get", Object.class);
                        System.out.println("headers type : " + headers.getClass().getName());
                        System.out.println("get = " + get);
                        get.setAccessible(true);
                        Object headerList = get.invoke(headers, LinkConstant.TRACE_ID);
                        System.out.println("headerList = " + headerList);

                        Object userAgent = get.invoke(headers, "User-Agent");
                        System.out.println("userAgent = " + userAgent);

                        // 如果 taceId 不存在则生成唯一 traceId 并且将其放入到请求头中
                        if(Objects.isNull(headerList)){
                            String uuid = UUID.randomUUID().toString();

                            RequestContext requestContext = new RequestContext(uuid, null, "(String) agent");
                            traceIdThreadLocal.set(uuid);
                            requestTtl.set(requestContext);

                            Method mutate = serverHttpRequestClazz.getMethod("mutate");
                            Object builder = mutate.invoke(serverHttpRequest);
                            Method header = builder.getClass().getMethod("header", String.class, String[].class);
                            header.setAccessible(true);

                            header.invoke(builder, LinkConstant.TRACE_ID, new String[]{uuid});

                            System.out.println("add traceId-----------------");
                            //System.out.println(agent);
                            System.out.println(header);
                        }
                        System.out.println("-----------------------");
                        System.out.println(traceIdThreadLocal.get());
                        System.out.println(requestTtl.get());
                        System.out.println("-----------------------");
                    }

                    @Override
                    protected void afterReturning(Advice advice) {
                        System.out.println("afterReturning---------------");
                        if (!advice.isProcessTop()) {
                            return;
                        }
                        System.out.println("before remove traceIdThreadLocal: " + traceIdThreadLocal.get());
                        System.out.println("before remove requestTtl: " + requestTtl.get());
                        traceIdThreadLocal.remove();
                        requestTtl.remove();
                        System.out.println("after remove traceIdThreadLocal: " + traceIdThreadLocal.get());
                        System.out.println("after remove requestTtl: " + requestTtl.get());
                    }

                    @Override
                    protected void afterThrowing(Advice advice) {
                        System.out.println("afterThrowing---------------");
                        if (!advice.isProcessTop()) {
                            return;
                        }
                        traceIdThreadLocal.remove();
                        requestTtl.remove();
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
                        System.out.println("headerTraceId: " + headerTraceId);
                        System.out.println("headerTraceIdByStr = " + headerTraceIdByStr);
                        System.out.println("headerTraceId is NULL: " + (headerTraceId == null));


                        if(headerTraceId != null){
                            traceIdThreadLocal.set((String) headerTraceId);
                            RequestContext requestContext = new RequestContext((String) headerTraceId, (String) headerSpanId, (String) getHeader(requestObj, "User-Agent"));
                            requestTtl.set(requestContext);
                            setAttribute(requestObj, LinkConstant.TRACE_ID, getThreadLocalValue());
                            System.out.println("11111");
                        }

                        if(traceIdThreadLocal.get() == null){
                            String uuid = UUID.randomUUID().toString();
                            RequestContext requestContext = new RequestContext(uuid, null, (String) getHeader(requestObj, "User-Agent"));
                            traceIdThreadLocal.set(uuid);
                            requestTtl.set(requestContext);
                            String requestURI = (String) getMethod(requestObj, "getRequestURI").invoke(requestObj);
                            logger.info("Injected traceId: {} into request: {}", uuid, requestURI);
                            System.out.println("Injected traceId:" + uuid);
                            System.out.println("request:" + requestURI);
                            System.out.println("00000");
                        }

                        System.out.println("-----------------------");
                        System.out.println(traceIdThreadLocal.get());
                        System.out.println(requestTtl.get());
                        System.out.println("-----------------------");
                    }

                    @Override
                    protected void afterReturning(Advice advice) {
                        System.out.println("afterReturning---------------");
                        if (!advice.isProcessTop()) {
                            return;
                        }
                        System.out.println("before remove traceIdThreadLocal: " + traceIdThreadLocal.get());
                        System.out.println("before remove requestTtl: " + requestTtl.get());
                        traceIdThreadLocal.remove();
                        requestTtl.remove();
                        System.out.println("after remove traceIdThreadLocal: " + traceIdThreadLocal.get());
                        System.out.println("after remove requestTtl: " + requestTtl.get());
                    }

                    @Override
                    protected void afterThrowing(Advice advice) {
                        System.out.println("afterThrowing---------------");
                        if (!advice.isProcessTop()) {
                            return;
                        }
                        traceIdThreadLocal.remove();
                        requestTtl.remove();
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
                        System.out.println("feign.SynchronousMethodHandler 的 executeAndDecode");
                        Object o = advice.getParameterArray()[0];
                        System.out.println("参数: " + o);

                        System.out.println("traceIdThreadLocal: " + traceIdThreadLocal.get());
                        header(o, LinkConstant.TRACE_ID, traceIdThreadLocal.get());
                        header(o, LinkConstant.SPAN_ID, requestTtl.get().getSpanId());
                        System.out.println("headers(o) = " + headers(o));
                        System.out.println("+++++++++++++++++++++++++++++++++++++++=");
                    }
                });

    }


    private Object header(Object obj, String name, String... values) {
        try {
            Method header = obj.getClass().getMethod("header", String.class, String[].class);
            return header.invoke(obj, name, values);
        } catch (Exception e) {
            return null;
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
