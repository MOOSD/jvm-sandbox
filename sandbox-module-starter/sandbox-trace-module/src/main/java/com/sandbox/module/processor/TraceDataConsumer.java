package com.sandbox.module.processor;

import cn.newgrand.ck.constant.ApiPathConstant;
import cn.newgrand.ck.executor.DataConsumer;
import cn.newgrand.ck.reporter.DataReporter;
import cn.newgrand.ck.reporter.HttpReporter;
import cn.newgrand.ck.reporter.LogReporter;
import com.alibaba.jvm.sandbox.api.resource.AgentInfo;
import com.alibaba.jvm.sandbox.api.resource.ConfigInfo;
import com.alibaba.jvm.sandbox.api.tools.ConcurrentHashSet;
import com.alibaba.jvm.sandbox.api.tools.HkUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sandbox.module.node.MethodInfo;
import com.sandbox.module.node.MethodTree;
import com.sandbox.module.node.TraceBaseInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TraceDataConsumer implements DataConsumer<MethodTree> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DataReporter dataReporter;

    private final AgentInfo agentInfo;

    private final ConfigInfo configInfo;

    private final ReentrantReadWriteLock.ReadLock readLock;
    private final ReentrantReadWriteLock.WriteLock writeLock;


    ObjectMapper objectMapper = new ObjectMapper();
    public TraceDataConsumer(ConfigInfo configInfo, AgentInfo agentInfo) {
        this.configInfo = configInfo;
        this.agentInfo = agentInfo;
        ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
        readLock = reentrantReadWriteLock.readLock();
        writeLock = reentrantReadWriteLock.writeLock();
        String coverageUrl = HkUtils.getUrl(configInfo.getHkServerIp(), configInfo.getHkServerPort(),
                ApiPathConstant.TRACE_REPORT_URL);
        this.dataReporter = new HttpReporter(coverageUrl);
    }

    @Override
    public void consume(MethodTree data) {
        logger.info("链路数据消费");
        // 获取类覆盖率
        try{
            TraceBaseInfo traceBaseInfo = new TraceBaseInfo();
            traceBaseInfo.setAgentId(agentInfo.getInstanceId());
            traceBaseInfo.setRequestCreateTime(data.getRequestCreateTime());
            traceBaseInfo.setTraceId(data.getTraceId());
            traceBaseInfo.setSpanId(data.getSpanId());
            traceBaseInfo.setRequestUrl(data.getRequestUri());
            traceBaseInfo.setSort(data.getSort());
            traceBaseInfo.setSortRpc(data.getSortRpc());
            traceBaseInfo.setRequestMethod(data.getRequestMethod());
            traceBaseInfo.setSimpleTree(data.convertToDTO(data.getCurrent(),data.getSort()));
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
        dataReporter.report(json);
        //
    }
}
