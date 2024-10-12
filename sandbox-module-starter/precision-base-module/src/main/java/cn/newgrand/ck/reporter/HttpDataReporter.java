package cn.newgrand.ck.reporter;

import com.alibaba.jvm.sandbox.api.tools.HttpClientUtil;
import com.alibaba.jvm.sandbox.api.tools.JSON;
import com.alibaba.jvm.sandbox.api.resource.ConfigInfo;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.util.function.Consumer;

public class HttpDataReporter extends DataReporter {

    private final ConfigInfo configInfo;
    private final String url;

    public HttpDataReporter(ConfigInfo configInfo, String url) {
        this.configInfo = configInfo;
        this.url = url;
    }

    @Override
    public void report(Object data, Consumer<HttpResponse> responseConsumer){
        try(CloseableHttpResponse closeableHttpResponse = reportByPost(data)){

            responseConsumer.accept(closeableHttpResponse);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private CloseableHttpResponse reportByPost(Object data) throws IOException {

            HttpPost httpPost = new HttpPost(url);
            // 设置请求头
            httpPost.setHeader("Content-Type","application/json;charset=UTF-8");
            // 构造请求体
            StringEntity stringEntity = new StringEntity(JSON.toJSONString(data));
            httpPost.setEntity(stringEntity);
            return HttpClientUtil.httpClient.execute(httpPost);

    }

}
