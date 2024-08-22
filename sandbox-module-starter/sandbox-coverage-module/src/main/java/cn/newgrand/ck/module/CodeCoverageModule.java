package cn.newgrand.ck.module;



import cn.newgrand.ck.pojo.MethodCoverage;
import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.LoadCompleted;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
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

    /**
     * 按照方法级别手机覆盖率信息
     */
    @Override
    public void loadCompleted() {
        log.info("类加载器1：{},类加载器2：{}",log.getClass().getClassLoader().toString(), moduleEventWatcher.toString());
        AdviceListener adviceListener = new AdviceListener() {
            final Map<String, MethodCoverage> coverageInfo = new LinkedHashMap<>();

            @Override
            protected void before(Advice advice) throws Throwable {
                advice.attach(new MethodCoverage());
                super.before(advice);

            }

            @Override
            protected void after(Advice advice) throws Throwable {
                super.after(advice);

            }

            @Override
            protected void beforeLine(Advice advice, int lineNum) {
                beforeLineHandle(advice, lineNum);//最重要的处理过程

            }
        };

        // 激活
        new EventWatchBuilder(moduleEventWatcher, EventWatchBuilder.PatternType.REGEX)//一定要选择这种表达式模式
                .onClass(buildClassPattern())//设置类的正则表达式
                .onAnyBehavior()
                .onWatching()
                .withLine()//有它，才能获取到行号
                .onWatch(adviceListener);
    }





    private void beforeLineHandle(Advice advice, int lineNum) {
        // 获取方法覆盖率信息


        //当前触发事件信息
        String currentBehavior = advice.getBehavior().getName();
        //方法名称
        String methodName = advice.getBehavior().getName();
        //获取实现方法的具体父类名称
        String className   = Objects.nonNull(advice.getTarget()) ? advice.getTarget().getClass().getName() : "null";
        log.info(lineNum + " className: "+className +" : " + currentBehavior);

    }

    //根据实际情况 构建匹配类的正则表达式
    private String buildClassPattern() {
        return "^cn\\.newgrand\\.ck.*";
    }




}
