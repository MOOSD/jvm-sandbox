package cn.newgrand.ck.module;



import cn.newgrand.ck.executor.*;
import cn.newgrand.ck.pojo.MethodCoverage;
import cn.newgrand.ck.tools.JSON;
import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.LoadCompleted;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ConfigInfo;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.*;

@MetaInfServices(Module.class)
@Information(id = "code-coverage", version = "0.0.1")
public class CodeCoverageModule implements Module, LoadCompleted {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Resource
    private ConfigInfo configInfo;

    private AsyncDataExecutor<MethodCoverage> dataExecutor;


    /**
     * 按照方法级别手机覆盖率信息
     */
    @Override
    public void loadCompleted() {
        initModule();

        // 创建Listener
        AdviceListener adviceListener = new AdviceListener() {

            @Override
            protected void before(Advice advice) {
                MethodCoverage methodCoverage = new MethodCoverage();
                String methodName = advice.getBehavior().getName();
                String className = Objects.nonNull(advice.getTarget()) ? advice.getTarget().getClass().getName() : "null";
                methodCoverage.setClassName(className);
                methodCoverage.setMethodName(methodName);
//                methodCoverage.setParameter(advice.getParameterArray());

                advice.attach(methodCoverage);
            }

            @Override
            protected void after(Advice advice) {
                dataExecutor.put(advice.attachment());
            }

            @Override
            protected void beforeLine(Advice advice, int lineNum) {
                MethodCoverage coverage = advice.attachment();
                coverage.recode(lineNum);
            }
        };

        // 开启监听
        new EventWatchBuilder(moduleEventWatcher, EventWatchBuilder.PatternType.REGEX)//一定要选择这种表达式模式
                .onClass(buildClassPattern())//设置类的正则表达式
                .onAnyBehavior()
                .onWatching()
                .withLine()//有它，才能获取到行号
                .onWatch(adviceListener);
        // 激活executor
        dataExecutor.active();
    }

    //构建匹配类的正则表达式
    private String buildClassPattern() {
        return "^cn\\.newgrand\\.ck.*";
    }

    private void initModule(){
        log.info("覆盖率模块加载中");
        // 初始化dataExecutor
        dataExecutor = new AsyncDataExecutor<>(1000,2, new CoverageDataConsumerBuilder(configInfo));
    }

}
