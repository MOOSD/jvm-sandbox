package com.sandbox.module.processor;

import cn.newgrand.ck.executor.DataConsumer;
import cn.newgrand.ck.reporter.DataReporter;
import com.alibaba.jvm.sandbox.api.resource.AgentInfo;
import com.alibaba.jvm.sandbox.api.resource.ConfigInfo;
import com.alibaba.jvm.sandbox.api.tools.ConcurrentHashSet;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sandbox.module.node.MethodTree;
import com.sandbox.module.node.MethodTreeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CoverageDataConsumer implements DataConsumer<MethodTree> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DataReporter dataReporter;

    private final ConcurrentHashSet<String> sendClassCoverage = new ConcurrentHashSet<>();

    private final ReentrantReadWriteLock.WriteLock writeLock;

    public CoverageDataConsumer(ConfigInfo configInfo, DataReporter dataReporter, AgentInfo agentInfo) {
        this.dataReporter = dataReporter;
        ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
        this.writeLock = reentrantReadWriteLock.writeLock();
    }

    @Override
    public void consume(MethodTree data) {
        logger.info("链路数据消费");
        // 获取类覆盖率
        try{
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            String json = objectMapper.writeValueAsString(data.convertToDTO(data.getCurrent()));
            sendClassCoverage.add(json);
        }catch (Exception e){
            logger.error("链路数据消费异常",e);
        }
        // 尝试发送数据
        tryReport();
    }

    private void tryReport(){
        try {
            writeLock.lock();
            if (sendClassCoverage.size() > 2) {
                dataReporter.report(sendClassCoverage);
                sendClassCoverage.clear();
            }
        } finally {
            writeLock.unlock();
        }

    }
}
