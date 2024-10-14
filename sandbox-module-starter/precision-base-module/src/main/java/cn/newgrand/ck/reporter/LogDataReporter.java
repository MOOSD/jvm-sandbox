package cn.newgrand.ck.reporter;

import com.alibaba.jvm.sandbox.api.tools.JSON;
import com.alibaba.jvm.sandbox.api.resource.ConfigInfo;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Consumer;

public class LogDataReporter extends DataReporter<Object> {
    private final Logger logger = LoggerFactory.getLogger(LogDataReporter.class);
    private final ConfigInfo configInfo;

    public LogDataReporter(ConfigInfo configInfo) {
        this.configInfo = configInfo;
    }

    public Object report(Object data){
        logger.info("处理后的数据:{}", JSON.toJSONString(data));
        return null;
    }
}
