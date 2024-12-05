package cn.newgrand.ck.pojo;

import com.alibaba.jvm.sandbox.api.tools.ConcurrentHashSet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 方法的覆盖率信息
 */
public class ClassCoverage {

    private String ClassName;
    /**
     * 使用hashSet去重
     */
    private final ConcurrentHashMap<Integer, AtomicInteger> coverageLineMap = new ConcurrentHashMap<>();

    public ClassCoverage(String className) {
        ClassName = className;
    }

    /**
     * 记录覆盖率
     */
    public void recordCoverage(int line){
        AtomicInteger counter = coverageLineMap.computeIfAbsent(line, key -> new AtomicInteger(0));
        counter.incrementAndGet();
    }

    public String getClassName() {
        return ClassName;
    }

    public void setClassName(String className) {
        ClassName = className;
    }

    public Map<Integer, AtomicInteger> getCoverageLineMap() {
        return coverageLineMap;
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
