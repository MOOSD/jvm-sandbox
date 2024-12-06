package cn.newgrand.ck.pojo;

import cn.newgrand.ck.entity.request.BaseCoverageDateReportRequest;

import java.util.Set;

public class CoverageDateReportRequest extends BaseCoverageDateReportRequest {

    protected Set<ClassCoverage> classCoverageCollection;

    public Set<ClassCoverage> getClassCoverageCollection() {
        return classCoverageCollection;
    }

    public void setClassCoverageCollection(Set<ClassCoverage> classCoverageCollection) {
        this.classCoverageCollection = classCoverageCollection;
    }
}
