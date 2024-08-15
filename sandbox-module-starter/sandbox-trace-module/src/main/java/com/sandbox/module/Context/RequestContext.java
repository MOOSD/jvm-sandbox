package com.sandbox.module.Context;

public class RequestContext {

    private String traceId;

    private String message;

    public RequestContext(String traceId, String message) {
        this.traceId = traceId;
        this.message = message;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
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
                ", message='" + message + '\'' +
                '}';
    }
}
