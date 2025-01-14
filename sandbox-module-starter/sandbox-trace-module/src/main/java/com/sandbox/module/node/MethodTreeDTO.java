package com.sandbox.module.node;

import java.util.ArrayList;
import java.util.LinkedList;
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

    private String behavior;
    private String target;
    private String returnType;

    private String[] params;
    private Integer parentSort;
    private Integer depth;
    private Integer sort;
    private long beginTimestamp;
    private long endTimestamp;
    private List<String> methodCell = new LinkedList<>();
    private List<Integer> childrenSort = new ArrayList<>();

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

    public List<Integer> getChildrenSort() {
        return childrenSort;
    }

    public void setChildren(List<Integer> children) {
        this.childrenSort = children;
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

    public List<String> getMethodCell() {
        return methodCell;
    }
    public MethodTreeDTO(){
        this.childrenSort = new ArrayList<>();
    }

    public void setMethodCell(List<String> methodCell) {
        this.methodCell = methodCell;
    }

    public Integer getParentSort() {
        return parentSort;
    }

    public void setParentSort(Integer parentSort) {
        this.parentSort = parentSort;
    }

    public void addChild(Integer child) {
        this.childrenSort.add(child);
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

    public String[] getParams() {
        return params;
    }

    public void setParams(String[] params) {
        this.params = params;
    }
}
