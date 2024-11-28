package com.sandbox.module.node;

import java.util.Arrays;

public class MethodInfo {

    private String className;
    private String methodName;
    private String[] annotations;

    private String log;

    private Boolean isSend = false;

    private String data;

    private String[] params;


    private String traceId;

    private String spanId;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }


    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getLog(){
        return log;
    }

    public void setLog(String log){
        this.log = log;
    }
    public String getData(){
        return data;
    }

    public void setData(String data){
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void                                                                                                                                                                                                                                         setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }

    @Override
    public String toString() {
        return "MethodInfo{" +
                "className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", log='" + log + '\'' +
                ", data='" + data + '\'' +
                ", traceId='" + traceId + '\'' +
                ", spanId='" + spanId + '\'' +
                ", annotation=" + Arrays.toString(annotations) +
                '}';
    }

    public String[] getParams() {
        return params;
    }

    public void setParams(String[] params) {
        this.params = params;
    }

    public String[] getAnnotations() {
        return annotations;
    }

    public void setAnnotations(String[] annotation) {
        this.annotations = annotation;
    }

    public Boolean getSend() {
        return isSend;
    }

    public void setSend(Boolean send) {
        isSend = send;
    }
}
