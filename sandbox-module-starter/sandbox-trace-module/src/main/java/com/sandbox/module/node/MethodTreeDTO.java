package com.sandbox.module.node;

import java.util.ArrayList;
import java.util.List;

public class MethodTreeDTO {


    private String clizzName;

    private String methodName;

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    private Integer depth;
    private Integer sort;
    private long beginTimestamp;
    private long endTimestamp;
    private List<MethodTreeDTO> children = new ArrayList<>();


    public long getBeginTimestamp() {
        return beginTimestamp;
    }

    public void setBeginTimestamp(long beginTimestamp) {
        this.beginTimestamp = beginTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public List<MethodTreeDTO> getChildren() {
        return children;
    }

    public void setChildren(List<MethodTreeDTO> children) {
        this.children = children;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public String getClizzName() {
        return clizzName;
    }

    public void setClizzName(String clizzName) {
        this.clizzName = clizzName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
}
