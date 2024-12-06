package cn.newgrand.ck.reporter;

import com.alibaba.jvm.sandbox.api.tools.JSON;

/**
 * 将数据序列化为JSON字符串后的报告器
 */
public class JsonSerializeReporter extends DataReporter {

    private final DataReporter delegate;

    public JsonSerializeReporter(DataReporter delegate) {
        this.delegate = delegate;
    }

    @Override
    public ReportResult report(Object data) {
        String jsonString = JSON.toJSONString(data);
        return delegate.report(jsonString);
    }
}
