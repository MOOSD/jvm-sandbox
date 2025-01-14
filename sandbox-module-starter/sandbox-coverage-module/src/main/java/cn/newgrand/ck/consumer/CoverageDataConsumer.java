package cn.newgrand.ck.consumer;

import cn.newgrand.ck.constant.ApiPathConstant;
import cn.newgrand.ck.executor.DataConsumer;
import cn.newgrand.ck.pojo.ClassCoverage;
import cn.newgrand.ck.pojo.CoverageDateReportRequest;
import cn.newgrand.ck.pojo.MethodCoverage;
import cn.newgrand.ck.reporter.DataReporter;
import cn.newgrand.ck.reporter.HttpReporter;
import cn.newgrand.ck.reporter.LogReporter;
import com.alibaba.jvm.sandbox.api.resource.AgentInfo;
import com.alibaba.jvm.sandbox.api.resource.ConfigInfo;
import com.alibaba.jvm.sandbox.api.tools.HkUtils;
import com.alibaba.jvm.sandbox.api.tools.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * 要发送的
     */
    private final ConcurrentHashMap<String, ClassCoverage> sendClassCoverage = new ConcurrentHashMap<>();


    @Override
    public void consume(MethodCoverage data) {
        logger.debug("数据消费:{}", data.toString());
        // todo 为什么方法名拿不到？暂时忽略为null的方法名
        if (data.getCoverageLine().isEmpty() || "null".equals(data.getClassName())){
            logger.warn("数据消费异常:{}",data);
            return;
        }
        // 获取类覆盖率
        String className = data.getClassName();
        try{
            readLock.lock();
            ClassCoverage classCoverage = sendClassCoverage.computeIfAbsent(className, ClassCoverage::new);
            for (Integer i : data.getCoverageLine()) {
                classCoverage.recordCoverage(i);
            }
        }finally {
            readLock.unlock();
        }
        readyToSend();
    }

    /**
     * 消费停止时，再次发送数据
     */
    @Override
    public void stop() {
        // 停止消费
        logger.info("上报剩余的 {} 条数据", sendClassCoverage.size());
        sendData();
    }


    /**
     * 阻塞的发送覆盖率数据
     * 发送数据(对sendClassCoverage的重置，因此必须是同步的)
     */
    private void readyToSend(){
        String sendJson = null;
        try{
            writeLock.lock();
            if (sendClassCoverage.size() > 5) {
                CoverageDateReportRequest coverageDateReportRequest = new CoverageDateReportRequest();
                coverageDateReportRequest.setInstanceId(agentInfo.getInstanceId());
                coverageDateReportRequest.setClassCoverageDataCollection(sendClassCoverage.values());
                sendJson = JSON.toJSONString(coverageDateReportRequest);
                sendClassCoverage.clear();
            }
        }finally {
            writeLock.unlock();
        }

        if(sendJson != null){
            dataReporter.report(sendJson);
        }

    }

    private void sendData(){
        if (sendClassCoverage.isEmpty()){
            return;
        }
        CoverageDateReportRequest coverageDateReportRequest = new CoverageDateReportRequest();
        coverageDateReportRequest.setInstanceId(agentInfo.getInstanceId());
        // 发送信息
        coverageDateReportRequest.setClassCoverageDataCollection(sendClassCoverage.values());
        String jsonString = JSON.toJSONString(coverageDateReportRequest);
        sendClassCoverage.clear();
        dataReporter.report(jsonString);
    }



    public CoverageDataConsumer(ConfigInfo configInfo, AgentInfo agentInfo) {
        System.out.println(System.getProperties());
        this.configInfo = configInfo;
        this.agentInfo = agentInfo;
        ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
        readLock = reentrantReadWriteLock.readLock();
        writeLock = reentrantReadWriteLock.writeLock();
        String coverageUrl = HkUtils.getUrl(configInfo.getHkServerIp(), configInfo.getHkServerPort(),
                ApiPathConstant.COVERAGE_REPORT_URL);
        this.dataReporter = new LogReporter(new HttpReporter(coverageUrl));;

    }

}

