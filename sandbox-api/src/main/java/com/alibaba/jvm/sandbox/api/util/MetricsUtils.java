package com.alibaba.jvm.sandbox.api.util;


import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.TimeUnit;


public class MetricsUtils {
    private final static Logger logger = LoggerFactory.getLogger(MetricsUtils.class);


    private static PrometheusMeterRegistry prometheusRegistry = null;


    private static Timer httpTimer;

    private static boolean notInit = true;

    /**
     * 私有化构造器
     */
    private MetricsUtils() {
    }

    public static PrometheusMeterRegistry getPrometheusRegistry() {
        return prometheusRegistry;
    }

    public static String getPrometheusText(){
        if (notInit){
            return null;
        }
        return prometheusRegistry.scrape();
    }

    /**
     * 初始化工具类
     */
    public static void init(){
        if (!notInit){
            logger.warn("指标工具类已被初始化！");
            return;
        }
        // 初始化Registry
        PrometheusConfig config = new PrometheusConfig() {
            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public Properties prometheusProperties() {
                Properties properties = new Properties();
                properties.putAll(PrometheusConfig.super.prometheusProperties());
                properties.setProperty("io.prometheus.exemplars.sampleIntervalMilliseconds", "1");
                return properties;
            }
        };

        prometheusRegistry = new PrometheusMeterRegistry(config, new PrometheusRegistry(), Clock.SYSTEM);
        // 注册指标
        //new TomcatMetrics().bindTo(prometheusRegistry);
        // 文件描述
        new FileDescriptorMetrics().bindTo(prometheusRegistry);
        // 启动时间
        new UptimeMetrics().bindTo(prometheusRegistry);
        // 类加载情况
        new ClassLoaderMetrics().bindTo(prometheusRegistry);
        // 监控内存使用情况。
        new JvmMemoryMetrics().bindTo(prometheusRegistry);
        // 监控垃圾回收信息。
        new JvmGcMetrics().bindTo(prometheusRegistry);
        // 监控线程信息。
        new JvmThreadMetrics().bindTo(prometheusRegistry);
        // 监控 CPU 使用率和核心数。
        new ProcessorMetrics().bindTo(prometheusRegistry);

        // 初始化meter


        httpTimer = Timer.builder("http_server_requests_seconds")
                .description("Time taken to process HTTP requests")
                .register(prometheusRegistry);
        notInit = false;
    }

    /**
     * 记录http接口的执行时间
     * @param executeTime 执行耗时，单位毫秒
     */
    public static void httpTimeRecord(Long executeTime){
        if (notInit){
            return;
        }

        httpTimer.record(executeTime, TimeUnit.MILLISECONDS);
    }


    /**
     * 记录http接口请求次数,每次调用counter+1
     *
     */
    public static void httpCounterIncrement(String... tags){
        if (notInit){
            return;
        }
        Counter httpCounter = Counter.builder("http_server_requests_seconds_count")
                .tags(tags)
                .register(prometheusRegistry);

        httpCounter.increment();

    }


    public static void httpCounterIncrement(String responseCode){
        httpCounterIncrement("status",responseCode);
    }
}
