package cn.newgrand.ck.executor;

import java.util.UUID;

/**
 * 数据消费者
 * @param <T> 要消费的数据类型
 */
public abstract class DataConsumer<T> {

    public final String id = UUID.randomUUID().toString();

    /**
     * 数据处理器的回调
     */
    protected ProcessorCallBack<T> processorCallBack;

    /**
     * 数据处理逻辑
     */
    abstract public void consume(T data);

    /**
     * 处理器是否可用
     */
    abstract public boolean isAvailable();

}
