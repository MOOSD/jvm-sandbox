package com.sandbox.module.domain;

import java.util.ArrayList;
import java.util.List;

public class RequestContext {

    private String traceId;
    private String spanId;
    private String message;
    private String requestUrl;
    private String requestMethod;
    private String[] clazzApiPath;
    private String[] methodApiPath;

    // 链路数据内部通信属性
    private Integer sort;
    private List<Integer> sortRpc;
    private Long requestCreateTime;

    public RequestContext(String traceId, String spanId, String message, String requestUrl,String requestMethod,long requestCreateTime) {
        this.traceId = traceId;
        this.requestUrl = requestUrl;
        if(spanId != null){
            this.spanId = spanId;
        } else {
            this.spanId = "root";
        }
        this.message = message;
        this.requestMethod = requestMethod;
        this.sort = 0;
        this.sortRpc = new ArrayList<>();
        this.requestCreateTime = requestCreateTime;
    }
    // sortRpc增加
    public void addSortRpc(Integer sort){
        this.sortRpc.add(sort);
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


    @Override
    public String toString() {
        return "RequestContext{" +
                "traceId='" + traceId + '\'' +
                ", spanId='" + spanId + '\'' +
                ", message='" + message + '\'' +
                '}';
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }


    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public List<Integer> getSortRpc() {
        return sortRpc;
    }

    public void setSortRpc(List<Integer> sortRpc) {
        this.sortRpc = sortRpc;
    }

    public Long getRequestCreateTime() {
        return requestCreateTime;
    }

    public void setRequestCreateTime(Long requestCreateTime) {
        this.requestCreateTime = requestCreateTime;
    }

    public String[] getClazzApiPath() {
        return clazzApiPath;
    }

    public void setClazzApiPath(String[] classApiPath) {
        this.clazzApiPath = classApiPath;
    }

    public String[] getMethodApiPath() {
        return methodApiPath;
    }

    public void setMethodApiPath(String[] methodApiPath) {
        this.methodApiPath = methodApiPath;
    }
}
