package cn.newgrand.ck.executor;

/**
 * Processor的回调接口
 */
public interface ConsumerCallBack<T> {

    /**
     * 停止数据处理
     * @param consumer 调用此方法的处理器实例
     */
    void stopProcess(DataConsumer<T> consumer);

    /**
     * 停止数据处理
     * @param consumer 调用此方法的处理器实例
     */
    void restartProcess(DataConsumer<T> consumer);


}
