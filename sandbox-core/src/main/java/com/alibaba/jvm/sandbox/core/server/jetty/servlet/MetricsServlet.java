package com.alibaba.jvm.sandbox.core.server.jetty.servlet;


import com.alibaba.jvm.sandbox.api.util.MetricsUtils;
import com.alibaba.jvm.sandbox.core.CoreConfigure;
import com.alibaba.jvm.sandbox.core.manager.CoreModuleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 暴漏Metrics端口的servlet
 */
public class MetricsServlet extends HttpServlet {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    CoreConfigure configure;

    CoreModuleManager coreModuleManager;

    public MetricsServlet(CoreConfigure cfg, CoreModuleManager coreModuleManager) {
        configure = cfg;
        this.coreModuleManager = coreModuleManager;
        // 初始化指标工具类
        MetricsUtils.init();
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String scrape = MetricsUtils.getPrometheusText();
        if (scrape == null){
            return;
        }

        resp.setCharacterEncoding(configure.getServerCharset().name());
        resp.setContentType("text/plain");
        resp.getWriter().write(scrape);
    }





}
