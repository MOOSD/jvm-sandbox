package com.sandbox.module.node;

public class MethodInfo {

    private String className;
    private String methodName;

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
                ", traceId='" + traceId + '\'' +
                ", spanId='" + spanId + '\'' +
                '}';
    }
}
