package cn.newgrand.ck.pojo;

import cn.newgrand.ck.entity.request.BaseCoverageDateReportRequest;

import java.util.Collection;
import java.util.Set;

public class CoverageDateReportRequest extends BaseCoverageDateReportRequest {

    protected Collection<ClassCoverage> classCoverageCollection;

    public Collection<ClassCoverage> getClassCoverageCollection() {
        return classCoverageCollection;
    }

    public void setClassCoverageCollection(Collection<ClassCoverage> classCoverageCollection) {
        this.classCoverageCollection = classCoverageCollection;
    }
}
