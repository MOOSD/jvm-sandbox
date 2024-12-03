package cn.newgrand.ck.reporter;

import com.alibaba.jvm.sandbox.api.tools.HttpClientUtil;
import com.alibaba.jvm.sandbox.api.tools.JSON;
import com.alibaba.jvm.sandbox.api.resource.ConfigInfo;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

public class HttpDataReporter extends DataReporter {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String url;

    public HttpDataReporter(String url) {
        this.url = url;
    }

    @Override
    public ReportResult report(Object data){

        try{
            HttpClientUtil.postByJson(url,data);

        } catch (IOException e) {
            logger.info("数据上报异常",e);
        }
        return null;
    }
}
