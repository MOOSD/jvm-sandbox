package cn.newgrand.ck.reporter;

import cn.newgrand.ck.tools.JSON;
import com.alibaba.jvm.sandbox.api.resource.ConfigInfo;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class LogDataReporter extends DataReporter {
    private final Logger logger = LoggerFactory.getLogger(LogDataReporter.class);
    private final ConfigInfo configInfo;

    public LogDataReporter(ConfigInfo configInfo) {
        this.configInfo = configInfo;
    }

    @Override
    public void report(Object data) {
        report(data,(response)->{});
    }

    @Override
    public void report(Object data, Consumer<HttpResponse> responseConsumer){
        logger.info("处理后的数据:{}", JSON.toJSONString(data));
    }
}
