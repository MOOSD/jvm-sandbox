package cn.newgrand.ck.module;


import cn.newgrand.ck.executor.DataProcessor;
import cn.newgrand.ck.pojo.MethodCoverage;
import cn.newgrand.ck.processor.CoverageDataConsumer;
import cn.newgrand.ck.reporter.DataReporter;
import cn.newgrand.ck.reporter.LogDataReporter;
import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.LoadCompleted;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.AgentInfo;
import com.alibaba.jvm.sandbox.api.resource.ConfigInfo;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import org.apache.commons.codec.binary.StringUtils;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.Objects;

@MetaInfServices(Module.class)
@Information(id = "code-coverage", version = "0.0.1")
public class CodeCoverageModule implements Module, LoadCompleted {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Resource
    private ConfigInfo configInfo;

    @Resource
    private AgentInfo agentInfo;

    private DataProcessor<MethodCoverage> dataProcessor;

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
                // todo 过滤部分方法
                MethodCoverage methodCoverage = new MethodCoverage();
                String methodName = advice.getBehavior().getName();
                String className = Objects.nonNull(advice.getTarget()) ? advice.getTarget().getClass().getName() : "null";
                methodCoverage.setClassName(className);
                methodCoverage.setMethodName(methodName);
//                methodCoverage.setParameter(advice.getParameterArray());

                advice.attach(methodCoverage);
            }

            @Override
            protected void beforeLine(Advice advice, int lineNum) {
                MethodCoverage coverage = advice.attachment();
                coverage.recode(lineNum);
            }

            @Override
            protected void after(Advice advice) {
                dataProcessor.add(advice.attachment());
            }


        };
        // 如果服务器状态可用，则启用数据处理器
        if(agentInfo.hKServiceIsAvailable()){
            dataProcessor.enable();
        }

        // 开启监听
        new EventWatchBuilder(moduleEventWatcher, EventWatchBuilder.PatternType.REGEX)//一定要选择这种表达式模式
                .onClass(buildClassPattern())//设置类的正则表达式
                .onAnyBehavior()
                .onWatching()
                .withLine()//有它，才能获取到行号
                .onWatch(adviceListener);
        log.info("覆盖率模块加载完成");
    }

    @Override
    public void hkServerStateChange(boolean available) {
        log.info("鹰眼服务端状态发生变化：{}",available);
        if (available){
            dataProcessor.enable();
        }else
            dataProcessor.disable();
    }

    //构建匹配类的正则表达式
    private String buildClassPattern() {
        String moduleCoveragePattern = configInfo.getModuleCoveragePattern();
        if (Objects.isNull(moduleCoveragePattern) || moduleCoveragePattern.isEmpty()){
            log.info("未指定项目所用类通配符，使用默认通配符");
            return "^cn\\.newgrand.*";
        }
        log.info("覆盖率所用的类通配符是:{}",moduleCoveragePattern);
        return moduleCoveragePattern;
    }

    private void initModule(){
        log.info("覆盖率模块加载开始");

        // 创建消费者
        CoverageDataConsumer coverageDataConsumer = new CoverageDataConsumer(configInfo, agentInfo);

        // 创建数据消费者
        this.dataProcessor = new DataProcessor<>(2,10240, coverageDataConsumer);
    }

}
