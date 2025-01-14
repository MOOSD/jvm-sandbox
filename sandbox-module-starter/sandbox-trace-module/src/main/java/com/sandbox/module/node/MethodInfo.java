package com.sandbox.module.node;

import java.util.Arrays;
import java.util.Objects;

public class MethodInfo {

    private String className;
    private String methodName;
    private String[] annotations;
    private Integer sort;
    private String behavior;
    private String target;
    private String returnType;

    private String log;

    private Boolean isSend = false;

    private String data;

    private String[] params;

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

    /**
     * 复制log和data属性
     * @param methodInfo
     */
    public void mergeInfo(MethodInfo methodInfo){
        if(methodInfo == null){
            return;
        }
        if(Objects.nonNull(methodInfo.getLog())){
            this.setLog(methodInfo.getLog());
        }
        if(Objects.nonNull(methodInfo.getData())){
            this.setData(methodInfo.getData());
        }
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public String getBehavior() {
        return behavior;
    }

    public void setBehavior(String behavior) {
        this.behavior = behavior;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }
}
