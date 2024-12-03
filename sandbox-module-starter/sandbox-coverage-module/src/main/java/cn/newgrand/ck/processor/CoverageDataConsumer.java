package cn.newgrand.ck.processor;

import cn.newgrand.ck.constant.ApiPathConstant;
import cn.newgrand.ck.entity.request.CoverageDateReportRequest;
import cn.newgrand.ck.executor.DataConsumer;
import cn.newgrand.ck.pojo.ClassCoverage;
import cn.newgrand.ck.reporter.DataReporter;
import cn.newgrand.ck.pojo.MethodCoverage;
import cn.newgrand.ck.reporter.HttpDataReporter;
import cn.newgrand.ck.reporter.LogDataReporter;
import com.alibaba.jvm.sandbox.api.resource.AgentInfo;
import com.alibaba.jvm.sandbox.api.tools.ConcurrentHashSet;
import com.alibaba.jvm.sandbox.api.resource.ConfigInfo;
import com.alibaba.jvm.sandbox.api.tools.HkUtils;
import com.alibaba.jvm.sandbox.api.tools.HttpClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class CoverageDataConsumer implements DataConsumer<MethodCoverage> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DataReporter dataReporter;
    /**
     * 配置类信息
     */
    private final ConfigInfo configInfo;
    /**
     * agent信息
     */
    private final AgentInfo agentInfo;


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


    @Override
    public void consume(MethodCoverage data) {
        logger.info("数据消费");
        // 获取类覆盖率
        String className = data.getClassName();
        ClassCoverage classCoverage = appCoverage.computeIfAbsent(className, ClassCoverage::new);
        for (Integer i : data.getCoverageLine()) {
            boolean add = classCoverage.recordCoverage(i);
            // 如果有新增的未执行的代码行，则标记其为新增。
            if (add){
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
            if (sendClassCoverage.size() > 3) {
                CoverageDateReportRequest coverageDateReportRequest = new CoverageDateReportRequest();
                // 组装信息
                coverageDateReportRequest.setInstanceId(agentInfo.getInstanceId());
                // 发送信息
                HashMap<String, Set<Integer>> classCoverage = new HashMap<>();
                for (ClassCoverage coverage : sendClassCoverage) {
                    classCoverage.put(coverage.getClassName(), coverage.getCoverageLineSet());
                }
                coverageDateReportRequest.setClassCoverageMap(classCoverage);
                dataReporter.report(coverageDateReportRequest);
                sendClassCoverage.clear();
            }
        } finally {
            writeLock.unlock();
        }

    }

    public CoverageDataConsumer(ConfigInfo configInfo, AgentInfo agentInfo) {
        this.configInfo = configInfo;
        this.agentInfo = agentInfo;
        ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
        this.readLock = reentrantReadWriteLock.readLock();
        this.writeLock = reentrantReadWriteLock.writeLock();

        String coverageUrl = HkUtils.getUrl(configInfo.getHkServerIp(), configInfo.getHkServerPort(),
                ApiPathConstant.COVERAGE_REPORT_URL);

        // 创建数据发送器
        this.dataReporter = new LogDataReporter(configInfo);

    }

}

