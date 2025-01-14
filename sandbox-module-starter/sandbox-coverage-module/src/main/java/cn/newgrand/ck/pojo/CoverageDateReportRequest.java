package cn.newgrand.ck.pojo;

import cn.newgrand.ck.entity.request.BaseCoverageDateReportRequest;

import java.util.Collection;

public class CoverageDateReportRequest extends BaseCoverageDateReportRequest {

    protected Collection<ClassCoverage> classCoverageDataCollection;

    public Collection<ClassCoverage> getClassCoverageDataCollection() {
        return classCoverageDataCollection;
    }

    public void setClassCoverageDataCollection(Collection<ClassCoverage> classCoverageDataCollection) {
        this.classCoverageDataCollection = classCoverageDataCollection;
    }
}
