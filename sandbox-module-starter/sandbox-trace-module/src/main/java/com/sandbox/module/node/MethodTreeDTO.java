package com.sandbox.module.node;

import java.util.ArrayList;
import java.util.List;

public class MethodTreeDTO {

    private String data;
    private long beginTimestamp;
    private long endTimestamp;
    private List<MethodTreeDTO> children = new ArrayList<>();

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

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
}
