package cn.newgrand.ck.reporter;

import org.apache.http.HttpResponse;

import java.util.function.Consumer;

public abstract class DataReporter {

    public void report(Object data) {
        report(data,(response)->{});
    }

    public abstract void report(Object data, Consumer<HttpResponse> responseConsumer);


}
