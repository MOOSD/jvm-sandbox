package cn.newgrand.ck.pojo;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

/**
 * 方法的覆盖率信息
 */
public class MethodCoverage {

    private String className;

    private String methodName;

    private final HashMap<Integer, Boolean> booleanLinkedList = new LinkedHashMap<>();


    public void recode(int methodLine){
        booleanLinkedList.put(methodLine,true);
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
