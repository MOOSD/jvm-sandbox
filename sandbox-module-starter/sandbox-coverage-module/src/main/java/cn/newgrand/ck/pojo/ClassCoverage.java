package cn.newgrand.ck.pojo;

import cn.newgrand.ck.tools.ConcurrentHashSet;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 方法的覆盖率信息
 */
public class ClassCoverage {

    private String ClassName;
    /**
     * 使用hashSet去重
     */
    private final ConcurrentHashSet<Integer> coverageLineSet = new ConcurrentHashSet<>();

    public ClassCoverage(String className) {
        ClassName = className;
    }

    public boolean recordCoverage(int line){
        return coverageLineSet.add(line);
    }

    public String getClassName() {
        return ClassName;
    }

    public void setClassName(String className) {
        ClassName = className;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassCoverage that = (ClassCoverage) o;

        return ClassName.equals(that.ClassName);
    }

    @Override
    public int hashCode() {
        return ClassName.hashCode();
    }
}
