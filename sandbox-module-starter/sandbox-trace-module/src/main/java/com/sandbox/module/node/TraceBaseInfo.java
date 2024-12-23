package com.sandbox.module.node;

import java.util.List;

/**
 * 链路基本信息
 */
public class TraceBaseInfo {
    public String getSpanId() {
        return spanId;
    }

    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public MethodInfo[] getNodeDetail() {
        return nodeDetail;
    }

    public void setNodeDetail(MethodInfo[] nodeDetail) {
        this.nodeDetail = nodeDetail;
    }

    public List<MethodTreeDTO> getSimpleTree() {
        return simpleTree;
    }

    public void setSimpleTree(List<MethodTreeDTO> simpleTree) {
        this.simpleTree = simpleTree;
    }
    public List<Integer> getSortRpc() {
        return sortRpc;
    }

    public void setSortRpc(List<Integer> sortRpc) {
        this.sortRpc = sortRpc;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public Long getRequestCreateTime() {
        return requestCreateTime;
    }

    public void setRequestCreateTime(Long requestCreateTime) {
        this.requestCreateTime = requestCreateTime;
    }

    private String traceId;
    private String spanId;
    private String requestUrl;
    private Integer sort;
    private String agentId;
    private MethodInfo[] nodeDetail;
    private List<MethodTreeDTO> simpleTree;
    private List<Integer> sortRpc;
    private Long requestCreateTime;
    private String requestMethod;

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }
}
