package com.sandbox.module.domain;

public class RequestContext {

    private String traceId;

    private String spanId;
    private String message;

    private String requestUrl;


    private String requestMethod;

    public RequestContext(String traceId, String spanId, String message, String requestUrl,String requestMethod) {
        this.traceId = traceId;
        this.requestUrl = requestUrl;
        if(spanId != null){
            this.spanId = spanId;
        } else {
            this.spanId = "root";
        }
        this.message = message;
        this.requestMethod = requestMethod;
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
}
