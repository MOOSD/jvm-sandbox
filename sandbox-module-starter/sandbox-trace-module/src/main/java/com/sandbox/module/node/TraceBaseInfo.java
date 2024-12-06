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

    public MethodTreeDTO getSimpleTree() {
        return simpleTree;
    }

    public void setSimpleTree(MethodTreeDTO simpleTree) {
        this.simpleTree = simpleTree;
    }

    private String traceId;
    private String spanId;
    private String requestUrl;
    private Integer sort;
    private MethodInfo[] nodeDetail;
    private MethodTreeDTO simpleTree;
    private List<Integer> sortRpc;

    public List<Integer> getSortRpc() {
        return sortRpc;
    }

    public void setSortRpc(List<Integer> sortRpc) {
        this.sortRpc = sortRpc;
    }
}
