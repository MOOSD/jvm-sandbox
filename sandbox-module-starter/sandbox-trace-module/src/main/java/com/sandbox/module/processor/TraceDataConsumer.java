package com.sandbox.module.processor;

import cn.newgrand.ck.executor.DataConsumer;
import cn.newgrand.ck.reporter.DataReporter;
import com.alibaba.jvm.sandbox.api.resource.AgentInfo;
import com.alibaba.jvm.sandbox.api.resource.ConfigInfo;
import com.alibaba.jvm.sandbox.api.tools.ConcurrentHashSet;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sandbox.module.node.MethodInfo;
import com.sandbox.module.node.MethodTree;
import com.sandbox.module.node.TraceBaseInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TraceDataConsumer implements DataConsumer<MethodTree> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DataReporter dataReporter;


    private final ReentrantReadWriteLock.WriteLock writeLock;
    ObjectMapper objectMapper = new ObjectMapper();
    public TraceDataConsumer(ConfigInfo configInfo, DataReporter dataReporter, AgentInfo agentInfo) {
        this.dataReporter = dataReporter;
        ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
        this.writeLock = reentrantReadWriteLock.writeLock();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public void consume(MethodTree data) {
        logger.info("链路数据消费");
        // 获取类覆盖率
        try{
            TraceBaseInfo traceBaseInfo = new TraceBaseInfo();
            traceBaseInfo.setTraceId(data.getTraceId());
            traceBaseInfo.setSpanId(data.getSpanId());
            traceBaseInfo.setRequestUrl(data.getRequestUri());
            traceBaseInfo.setSort(data.getSort());
            traceBaseInfo.setSortRpc(data.getSortRpc());
            final MethodInfo[] methodInfoList = new MethodInfo[data.getSort()];
            traceBaseInfo.setSimpleTree(data.convertToDTO(data.getCurrent(),methodInfoList , data.getBaseInfo()));
            traceBaseInfo.setNodeDetail(methodInfoList);
            String json = objectMapper.writeValueAsString(traceBaseInfo);
            tryReport(json);
        }catch (Exception e){
            logger.error("链路数据消费异常",e);
        }

    }
    @Override
    public void stop() {

    }

    private void tryReport(String json){
        logger.info("json:{}",json);
    }
}
