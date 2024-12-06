package cn.newgrand.ck.reporter;

import com.alibaba.jvm.sandbox.api.tools.HttpClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class HttpReporter extends DataReporter {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String url;

    public HttpReporter(String url) {
        this.url = url;
    }

    @Override
    public ReportResult report(Object data){
        if (data == null){
            return ReportResult.error();
        }

        try{
            HttpClientUtil.postByJson(url, data);
            return ReportResult.success();
        } catch (IOException e) {
            logger.info("数据上报异常",e);
            return ReportResult.error();
        }
    }
}
