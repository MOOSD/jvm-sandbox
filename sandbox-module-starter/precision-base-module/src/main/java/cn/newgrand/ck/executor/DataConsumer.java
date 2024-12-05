package cn.newgrand.ck.executor;

import java.util.UUID;

/**
 * 数据消费者
 * @param <T> 要消费的数据类型
 */

public interface DataConsumer<T> {

//    /**
//     * 数据处理器的回调
//     */
//    protected ConsumerCallBack<T> consumerCallBack;

    /**
     * 数据处理逻辑，要求是线程安全的
     */
    void consume(T data);


    void stop();
}
