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
     * todo 删除此map，释放内存
     */
    private final ConcurrentHashMap<String, ClassCoverage> appClassCoverage = new ConcurrentHashMap<>();

    /**
     * 要发送的
     */
    private final ConcurrentHashSet<ClassCoverage> sendClassCoverage = new ConcurrentHashSet<>();


    @Override
    public void consume(MethodCoverage data) {
        logger.debug("数据消费:{}", data.toString());
        if (data.getCoverageLine().isEmpty()){
            return;
        }
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
        logger.info("上报剩余的 {} 条数据", sendClassCoverage.size());
        sendData();
    }


    /**
     * 阻塞的发送覆盖率数据
     * 发送数据(对sendClassCoverage的重置，因此必须是同步的)
     */
    private void readyToSend(ClassCoverage classCoverage){
        String sendJson = null;
        synchronized (sendClassCoverage) {
            sendClassCoverage.add(classCoverage);
            if (sendClassCoverage.size() > 5) {
                CoverageDateReportRequest coverageDateReportRequest = new CoverageDateReportRequest();
                coverageDateReportRequest.setInstanceId(agentInfo.getInstanceId());
                coverageDateReportRequest.setClassCoverageCollection(sendClassCoverage);
                sendJson = JSON.toJSONString(coverageDateReportRequest);
                sendClassCoverage.clear();
            }
        }

        if(sendJson != null){
            dataReporter.report(sendJson);
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
            coverageDateReportRequest.setClassCoverageCollection(sendClassCoverage);
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
        this.dataReporter = new LogReporter(new HttpReporter(coverageUrl));;

    }

}

