package cn.newgrand.ck.consumer;

import cn.newgrand.ck.constant.ApiPathConstant;
import cn.newgrand.ck.executor.DataConsumer;
import cn.newgrand.ck.pojo.ClassCoverage;
import cn.newgrand.ck.pojo.CoverageDateReportRequest;
import cn.newgrand.ck.pojo.MethodCoverage;
import cn.newgrand.ck.reporter.DataReporter;
import cn.newgrand.ck.reporter.HttpDataReporter;
import cn.newgrand.ck.reporter.LogDataReporter;
import com.alibaba.jvm.sandbox.api.resource.AgentInfo;
import com.alibaba.jvm.sandbox.api.resource.ConfigInfo;
import com.alibaba.jvm.sandbox.api.tools.ConcurrentHashSet;
import com.alibaba.jvm.sandbox.api.tools.HkUtils;
import com.alibaba.jvm.sandbox.api.tools.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;


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




    /**
     * 整个类的类覆盖率的map
     */
    private final ConcurrentHashMap<String, ClassCoverage> appClassCoverage = new ConcurrentHashMap<>();

    /**
     * 要发送的
     */
    private final ConcurrentHashSet<ClassCoverage> sendClassCoverage = new ConcurrentHashSet<>();


    @Override
    public void consume(MethodCoverage data) {
        logger.debug("数据消费");
        // 获取类覆盖率
        String className = data.getClassName();
        ClassCoverage classCoverage = appClassCoverage.computeIfAbsent(className, ClassCoverage::new);
        for (Integer i : data.getCoverageLine()) {
            classCoverage.recordCoverage(i);
        }
        readyToSend(classCoverage);
    }

    /**
     * 消费停止时，再次发送数据
     */
    @Override
    public void stop() {
        // 停止消费
        logger.info("覆盖率模块消费停止，剩余未更新的类覆盖率数量 {}",sendClassCoverage.size());
        sendData();
    }


    /**
     * 阻塞的发送覆盖率数据
     * 发送数据(对sendClassCoverage的重置，因此必须是同步的)
     */
    private void readyToSend(ClassCoverage classCoverage){
        if (classCoverage != null){
            sendClassCoverage.add(classCoverage);
        }

        synchronized (sendClassCoverage) {
            if (sendClassCoverage.size() > 50) {
                sendData();
            }
        }

    }

    private void sendData(){
        synchronized (sendClassCoverage) {
            if (sendClassCoverage.isEmpty()){
                return;
            }
            CoverageDateReportRequest coverageDateReportRequest = new CoverageDateReportRequest();
            coverageDateReportRequest.setInstanceId(agentInfo.getInstanceId());
            // 发送信息
            coverageDateReportRequest.setClassCoverage(sendClassCoverage);
            String jsonString = JSON.toJSONString(coverageDateReportRequest);
            sendClassCoverage.clear();
            dataReporter.report(jsonString);
        }
    }



    public CoverageDataConsumer(ConfigInfo configInfo, AgentInfo agentInfo) {
        System.out.println(System.getProperties());
        this.configInfo = configInfo;
        this.agentInfo = agentInfo;
        String coverageUrl = HkUtils.getUrl(configInfo.getHkServerIp(), configInfo.getHkServerPort(),
                ApiPathConstant.COVERAGE_REPORT_URL);

        // todo 更换Http发送器
        this.dataReporter = new LogDataReporter(configInfo);

    }

}

