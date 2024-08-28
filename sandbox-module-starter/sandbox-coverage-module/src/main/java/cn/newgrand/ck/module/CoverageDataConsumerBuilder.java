package cn.newgrand.ck.module;

import cn.newgrand.ck.executor.ConsumerBuilder;
import cn.newgrand.ck.executor.DataConsumer;
import cn.newgrand.ck.pojo.ClassCoverage;
import cn.newgrand.ck.reporter.DataReporter;
import cn.newgrand.ck.reporter.LogDataReporter;
import cn.newgrand.ck.pojo.MethodCoverage;
import cn.newgrand.ck.tools.ConcurrentHashSet;
import com.alibaba.jvm.sandbox.api.resource.ConfigInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class CoverageDataConsumerBuilder implements ConsumerBuilder<MethodCoverage> {

    private final DataReporter dataReporter;
    /**
     * 配置类信息
     */
    private final ConfigInfo configInfo;

    private final ReentrantReadWriteLock.ReadLock readLock;
    private final ReentrantReadWriteLock.WriteLock writeLock;

    /**
     * 类覆盖率的map
     */
    private final ConcurrentHashMap<String, ClassCoverage> appCoverage = new ConcurrentHashMap<>();

    /**
     * 要发送的
     */
    private final ConcurrentHashSet<ClassCoverage> sendClassCoverage = new ConcurrentHashSet<>();

    public CoverageDataConsumerBuilder(ConfigInfo configInfo) {
        this.configInfo = configInfo;
//            this.dataReporter = new HttpDataReporter(configInfo);
        this.dataReporter = new LogDataReporter(configInfo);
        ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
        this.readLock = reentrantReadWriteLock.readLock();
        this.writeLock = reentrantReadWriteLock.writeLock();
    }

    @Override
    public DataConsumer<MethodCoverage> getConsumer() {
        return new CoverageDataConsumer();
    }

    class CoverageDataConsumer implements DataConsumer<MethodCoverage> {

        @Override
        public void comsumer(MethodCoverage data) {
            // 获取类覆盖率
            String className = data.getClassName();
            ClassCoverage classCoverage = appCoverage.computeIfAbsent(className, ClassCoverage::new);
            for (Integer i : data.getCoverageLine()) {
                boolean add = classCoverage.recordCoverage(i);
                // 如果有新增的未执行的代码行，则标记其为新增。
                if (add && !sendClassCoverage.contains(classCoverage)){
                    sendClassCoverage.add(classCoverage);
                }
            }

            // 尝试发送数据
            tryReport();
        }

        /**
         * 发送数据
         */
        private void tryReport(){
            try {
                writeLock.lock();
                if (sendClassCoverage.size() > 20) {
                    dataReporter.report(sendClassCoverage);
                    sendClassCoverage.clear();
                }
            } finally {
                writeLock.unlock();
            }

        }
    }
}

