package cn.newgrand.ck.reporter;

import com.alibaba.jvm.sandbox.api.tools.JSON;
import com.alibaba.jvm.sandbox.api.resource.ConfigInfo;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Consumer;

public class LogDataReporter extends DataReporter {
    private final Logger logger = LoggerFactory.getLogger(LogDataReporter.class);
    private final ConfigInfo configInfo;

    public LogDataReporter(ConfigInfo configInfo) {
        this.configInfo = configInfo;
    }

    public ReportResult report(Object data){
        logger.info("上报的的数据:{}", JSON.toJSONString(data));
        return new ReportResult(true);
    }
}
