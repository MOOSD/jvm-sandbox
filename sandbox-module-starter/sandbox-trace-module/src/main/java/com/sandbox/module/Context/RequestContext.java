package com.sandbox.module.Context;

public class RequestContext {

    private String traceId;

    private String spanId;
    private String message;

    private String requestUrl;

    public RequestContext(String traceId, String spanId, String message, String requestUrl) {
        this.traceId = traceId;
        this.requestUrl = requestUrl;
        if(spanId != null){
            this.spanId = spanId + ".0";
        } else {
            this.spanId = "0";
        }
        this.message = message;
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
}
