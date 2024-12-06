package cn.newgrand.ck.reporter;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 给上报的数据额外的日志输出
 */
public class LogReporter extends DataReporter{

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DataReporter delegate;
    public LogReporter(DataReporter delegate) {
        this.delegate = delegate;
    }

    @Override
    public ReportResult report(Object data) {
        logger.info("数据上报信息:{}",data);
        return delegate.report(data);
    }
}
