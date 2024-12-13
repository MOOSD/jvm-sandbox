package cn.newgrand.ck.module;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.LoadCompleted;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.resource.AgentInfo;
import com.alibaba.jvm.sandbox.api.resource.ConfigInfo;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.*;

@MetaInfServices(Module.class)
@Information(id = "jvm-info", version = "0.0.1", author = "hts")
public class JvmInfoModule implements Module, LoadCompleted {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final HashSet<String> annotationSet = new HashSet<>();

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Resource
    private ConfigInfo configInfo;

    @Resource
    private AgentInfo agentInfo;


    @Override
    public void loadCompleted() {

//        new EventWatchBuilder(moduleEventWatcher, EventWatchBuilder.PatternType.REGEX)
//                .onClass("org.springframework.boot.SpringApplication")
//                .includeSubClasses()
//                .onBehavior("run")
//                .onWatching()
//                .withCall()
//                .onWatch(new AdviceListener() {
//
//                    private volatile boolean monitoringStarted = false;
//
//                    @Override
//                    protected void before(Advice advice) throws Throwable {
//                        if (!monitoringStarted) {
//                            synchronized (this) {
//                                if (!monitoringStarted) {
//                                    startJvmMonitoring();
//                                    monitoringStarted = true;
//                                }
//                            }
//                        }
//                    }
//
//                    public void startJvmMonitoring() {
//                        Timer timer = new Timer("JvmMonitorThread", true); // 守护线程模式
//
//                        // 每秒调度一次任务
//                        timer.scheduleAtFixedRate(new TimerTask() {
//                            @Override
//                            public void run() {
//                                try {
//                                    outputJvmMetrics();
//                                } catch (Exception e) {
//                                    logger.info("JVM 监控时发生错误: " + e.getMessage(), e);
//                                }
//                            }
//                        }, 0, 1000); // 延迟0ms启动，每隔1000ms运行一次
//                    }
//
//                    private void outputJvmMetrics() {
//                        // 内存使用情况
//                        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
//                        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
//                        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
//                        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
//                        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
//
//                        String jvm = "==================== JVM 指标 ====================\n" +
//                                "堆内存: 已使用 = " + heapMemoryUsage.getUsed() / 1024 / 1024 + " MB, 最大 = " + heapMemoryUsage.getMax() / 1024 / 1024 + " MB\n"+
//                                "非堆内存: 已使用 = " + nonHeapMemoryUsage.getUsed() / 1024 / 1024 + " MB, 最大 = " + nonHeapMemoryUsage.getMax() / 1024 / 1024 + " MB\n"+
//                                "线程数量: " + threadMXBean.getThreadCount()+"\n"+
//                                "守护线程数量: " + threadMXBean.getDaemonThreadCount()+"\n"+
//                                getGc()+
//                                "操作系统: " + osBean.getName() + " - 架构: " + osBean.getArch() + " - 系统负载: " + osBean.getSystemLoadAverage()+"\n"+
//                                "=================================================";
//                        logger.info(jvm);
//                    }
//
//                    private String getGc(){
//                        StringBuilder gc = new StringBuilder();
//                        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
//                            gc.append("GC: ").append(gcBean.getName()).append(" - 收集次数 = ").append(gcBean.getCollectionCount()).append(", 收集时间 = ").append(gcBean.getCollectionTime()).append(" ms\n0");
//                        }
//                        return gc.toString();
//                    }
//
//                });

    }
}
