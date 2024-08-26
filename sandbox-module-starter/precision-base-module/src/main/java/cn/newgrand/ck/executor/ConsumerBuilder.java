package cn.newgrand.ck.executor;

public interface ConsumerBuilder<T> {

    /**
     * 获取一个消费者实例
     */
    DataConsumer<T> getConsumer();
}
