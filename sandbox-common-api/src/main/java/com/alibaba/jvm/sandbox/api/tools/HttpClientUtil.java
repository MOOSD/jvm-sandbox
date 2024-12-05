package com.alibaba.jvm.sandbox.api.tools;


import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.HttpHost;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * HTTP客户端工具类
 */
public class HttpClientUtil {
    private final static int RETRY_COUNT = 3;
    private final static long RETRY_INTERVAL_TIME = 1000L;
    private final static int MAX_TOTAL = 1000;
    private final static int MAX_PER_ROUTE = 500;
    private final static int CONN_REQUEST_TIMEOUT = 5000;
    private final static int CONNECT_TIMEOUT = 8000;
    private final static int SOCKET_TIMEOUT = 200 * 1000;

    private static final Logger log = LoggerFactory.getLogger(HttpClientUtil.class);

    public static CloseableHttpClient httpClient;

    static {
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", SSLConnectionSocketFactory.getSocketFactory())
                .build();
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
        //设置整个连接池最大连接数
        connectionManager.setMaxTotal(MAX_TOTAL);
        //路由是对maxTotal的细分
        connectionManager.setDefaultMaxPerRoute(MAX_PER_ROUTE);
        //服务器返回数据(response)的时间，超过该时间抛出read timeout
        connectionManager.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(SOCKET_TIMEOUT).build());
        RequestConfig requestConfig = RequestConfig.custom()
                //连接上服务器(握手成功)的时间，超出该时间抛出connect timeout
                .setConnectTimeout(CONNECT_TIMEOUT)
                //从连接池中获取连接的超时时间，超过该时间未拿到可用连接，Timeout waiting for connection from pool
                .setConnectionRequestTimeout(CONN_REQUEST_TIMEOUT)
                .build();
        // 重试次数
        HttpClientBuilder client = getHttpClientBuilder();
        client.setDefaultRequestConfig(requestConfig);
        // 配置连接池
        client.setConnectionManager(connectionManager);
        httpClient = client.build();
    }


    private static HttpClientBuilder getHttpClientBuilder() {
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        // 只有io异常才会触发重试
        httpClientBuilder.setRetryHandler((IOException exception, int curRetryCount, HttpContext context) -> {
            // curRetryCount 每一次都会递增，从1开始
            if (curRetryCount > RETRY_COUNT) return false;
            try {
                //重试延迟
                Thread.sleep(curRetryCount * RETRY_INTERVAL_TIME);
            } catch (InterruptedException e) {
                log.error("请求异常",e);
            }
            if (exception instanceof ConnectTimeoutException ||
                    exception instanceof NoHttpResponseException ||
                    exception instanceof SocketException) {
                log.info("第{}次重试: ", curRetryCount);
                return true;
            }
            return false;
        });
        return httpClientBuilder;
    }

    public static <T>  T postByJson(String uri, Object requestBody, TypeReference<T> responseType) throws IOException {
        String responseJson = postByJsonStr(uri, requestBody);
        if (responseJson == null){
            return null;
        }
        return JSON.parseObject(responseJson,responseType);
    }

    public static void postByJson(String uri, Object requestBody) throws IOException {
        postByJsonStr(uri, requestBody);

    }

    public static <T>  T postByJson(String uri, Object requestBody, Class<T> responseType) throws IOException {
        String responseJson = postByJsonStr(uri, requestBody);
        if (responseJson == null){
            return null;
        }
        return JSON.parseObject(responseJson, responseType);
    }

    /**
     * @param requestBody 请求体如果是字符串，则不做序列化，请求体如果不是字符串，则序列化后发送
     */
    private static String postByJsonStr(String uri, Object requestBody) throws IOException{
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setHeader("Content-Type", "application/json");
        // 构造请求体

        String jsonString = requestBody instanceof String ? (String) requestBody :JSON.toJSONString(requestBody);

        httpPost.setEntity(new StringEntity(jsonString, ContentType.APPLICATION_JSON));
        StringBuilder responseBodyStr = new StringBuilder();
        try(CloseableHttpResponse response = httpClient.execute(httpPost)){
            // 如果响应非200，则抛出异常
            int statusCode = response.getStatusLine().getStatusCode();
            if (200 != statusCode) {
                throw new RuntimeException("请求异常"+ uri + " : " +statusCode);
            }
            // 无响应体直接返回
            if (response.getEntity() == null) {
                log.info("无响应体：{}", uri);
                return null;
            }
            InputStream responseInputStream = response.getEntity().getContent();
            InputStreamReader inputStreamReader = new InputStreamReader(responseInputStream, StandardCharsets.UTF_8);
            char[] charsBuffer = new char[200];
            int readLength;
            while((readLength = inputStreamReader.read(charsBuffer)) > 0){
                responseBodyStr.append(charsBuffer,0, readLength);
            }
            inputStreamReader.close();
        }
        log.info("URL:{},响应:{}",uri, responseBodyStr);
        return responseBodyStr.toString();
    }

    public static CloseableHttpResponse GetByQueryParam(String uri, Map<String,String> queryParam) throws IOException, URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(uri);
        if (Objects.nonNull(queryParam)){
            queryParam.forEach(uriBuilder::addParameter);
        }
        HttpGet httpGet = new HttpGet(uriBuilder.build());
        return httpClient.execute(httpGet);
    }
}
