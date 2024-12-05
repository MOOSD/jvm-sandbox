package cn.newgrand.ck.pojo;

import cn.newgrand.ck.entity.request.BaseCoverageDateReportRequest;

import java.util.Set;

public class CoverageDateReportRequest extends BaseCoverageDateReportRequest {

    protected Set<ClassCoverage> classCoverage;

    public Set<ClassCoverage> getClassCoverage() {
        return classCoverage;
    }

    public void setClassCoverage(Set<ClassCoverage> classCoverage) {
        this.classCoverage = classCoverage;
    }
}
