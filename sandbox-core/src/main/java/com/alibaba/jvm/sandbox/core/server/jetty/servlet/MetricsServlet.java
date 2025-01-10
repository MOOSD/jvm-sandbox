package com.alibaba.jvm.sandbox.core.server.jetty.servlet;


import com.alibaba.jvm.sandbox.core.CoreConfigure;
import com.alibaba.jvm.sandbox.core.manager.CoreModuleManager;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Properties;

/**
 * 暴漏Metrics端口的servlet
 */
public class MetricsServlet extends HttpServlet {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    CoreConfigure configure;

    CoreModuleManager coreModuleManager;

    PrometheusMeterRegistry prometheusRegistry;

    public MetricsServlet(CoreConfigure cfg, CoreModuleManager coreModuleManager) {
        configure = cfg;
        this.coreModuleManager = coreModuleManager;
        initPrometheus();
    }

    public void initPrometheus(){
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

        //  注册jvm相关指标
        new JvmMemoryMetrics().bindTo(prometheusRegistry);
        new JvmGcMetrics().bindTo(prometheusRegistry);
        new JvmThreadMetrics().bindTo(prometheusRegistry);
        new ProcessorMetrics().bindTo(prometheusRegistry);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String scrape = prometheusRegistry.scrape();
        resp.setCharacterEncoding(configure.getServerCharset().name());
        resp.setContentType("text/plain");
        resp.getWriter().write(scrape);
    }


}
