package cn.newgrand.ck.reporter;

/**
// * 异步的发送
 */
public class SyncReporter extends DataReporter{

    private final DataReporter delegate;

    public SyncReporter(DataReporter delegate) {
        this.delegate = delegate;
    }

    @Override
    public ReportResult report(Object data) {
        return delegate.report(data);
    }
}
