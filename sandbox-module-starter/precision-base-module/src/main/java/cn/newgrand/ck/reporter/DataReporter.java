package cn.newgrand.ck.reporter;

import org.apache.http.HttpResponse;

import java.util.function.Consumer;

public abstract class DataReporter<T> {

    public abstract T report(Object data);


}
