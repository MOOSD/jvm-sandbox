package cn.newgrand.ck.pojo;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Objects;

public class MethodCoverage {
    private String className;

    private String methodName;

//    private Object[] parameter;

    /**
     * 默认30行
     */
    private ArrayList<Integer> coverageLine = new ArrayList<>(30);

    public void recode(Integer line){
        coverageLine.add(line);
    }

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


    public ArrayList<Integer> getCoverageLine() {
        return coverageLine;
    }

    public void setCoverageLine(ArrayList<Integer> coverageLine) {
        this.coverageLine = coverageLine;
    }

    @Override
    public String toString() {
        return "MethodCoverage{" +
                "className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", coverageLine=" + coverageLine +
                '}';
    }
}
