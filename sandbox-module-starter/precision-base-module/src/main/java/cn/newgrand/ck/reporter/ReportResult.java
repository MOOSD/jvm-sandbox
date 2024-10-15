package cn.newgrand.ck.reporter;


public class ReportResult {

    boolean success;

    public ReportResult() {
    }

    public ReportResult(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
