package cn.newgrand.ck.pojo;

import java.util.ArrayList;
import java.util.BitSet;

/**
 * 方法的覆盖率信息
 */
public class ClassCoverage {

    private String className;

    private String methodName;

    private Object[] parameter;

    private BitSet coverage;


    public Object[] getParameter() {
        return parameter;
    }

    public void setParameter(Object[] parameter) {
        this.parameter = parameter;
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
}
