package cn.newgrand.ck.executor;


public interface DataConsumer<T> {


    /**
     * 每一个数据消费者的消费逻辑，会被持续的循环调用
     */
    void comsumer(T data);
}
