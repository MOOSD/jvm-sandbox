package cn.newgrand.ck.reporter;

import org.apache.http.HttpResponse;

import java.util.function.Consumer;

/**
 * 数据上报器
 */
public abstract class DataReporter {

    public abstract ReportResult report(Object data);


}
