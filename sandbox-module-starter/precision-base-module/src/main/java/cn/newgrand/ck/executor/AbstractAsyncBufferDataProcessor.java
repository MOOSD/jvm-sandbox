package cn.newgrand.ck.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异步带有缓冲的数据处理器
 * @param <T> 模块收集到的要处理的数据
 */
public abstract class AbstractAsyncBufferDataProcessor<T> implements ProcessorCallBack<T> {
    private final Logger log = LoggerFactory.getLogger(AbstractAsyncBufferDataProcessor.class);

    private final ThreadPoolExecutor consumerThreadPool;

    private final AsyncDataQueue<T> queue;

    protected AtomicInteger dataCount;

    private final int consumerCount;

    private final ConcurrentHashMap<DataConsumer<T>, CompletableFuture<Void>> consumerMap;


    /**
     * @param consumerCount 处理器实例数量
     * @param dataQueueSize 数据队列大小
     */
    public AbstractAsyncBufferDataProcessor(int consumerCount, int dataQueueSize) {
        this.consumerCount = consumerCount;
        consumerMap = new ConcurrentHashMap<>(consumerCount);
        this.dataCount = new AtomicInteger(0);
        consumerThreadPool = new ThreadPoolExecutor(consumerCount, consumerCount,
                0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>());

        this.queue = new AsyncDataQueue<T>(dataQueueSize);
    }

    /**
     * @param data 添加数据
     */
    public void add(T data){
        queue.put(data);
    }

    /**
     * 激活数据处理和消费
     */
    public void active(){
        // 创建消费线程
        for (int i = 0; i < consumerCount; i++) {
            // 实例化consumer
            DataConsumer<T> consumer = getConsumer();
            consumer.processorCallBack = this;
            CompletableFuture<Void> exceptionally = CompletableFuture.runAsync(whileTureDataConsumeRunnable(consumer), consumerThreadPool)
                    .exceptionally(throwable -> {
                        log.error("数据处理线程异常：", throwable);
                        return null;
                    });
            consumerMap.put(consumer,exceptionally);
        }
    }

    /**
     * 生成一个持续进行数据处理的处理器
     */
    public Runnable whileTureDataConsumeRunnable(DataConsumer<T> consumer){
        if (Objects.isNull(consumer)){
            return null;
        }
        return () -> {
            while(true){
                // 对中断进行响应,如果线程中断，则停止处理
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                // 异常不终止消费行为
                try{
                    T data = queue.get();
                    if (Objects.nonNull(data)){
                        consumer.consume(data);
                        dataCount.incrementAndGet();
                    }
                } catch (Throwable exception){
                    log.error("数据消费异常",exception);
                }
            }
        };
    }

    /**
     * 实现Process的回调方法
     */
    @Override
    public void stopProcess(DataConsumer<T> consumer) {
        CompletableFuture<Void> completableFuture = consumerMap.get(consumer);
        // 此consumer未曾执行
        if (completableFuture == null){
            return;
        }
        completableFuture.cancel(true);

    }

    @Override
    public void restartProcess(DataConsumer<T> consumer) {
        CompletableFuture<Void> exceptionally = CompletableFuture.runAsync(whileTureDataConsumeRunnable(consumer), consumerThreadPool)
                .exceptionally(throwable -> {
                    log.error("数据处理线程异常：", throwable);
                    return null;
                });
        consumerMap.put(consumer,exceptionally);
    }

    /**
     * 获取一个消费者
     */
    abstract public DataConsumer<T> getConsumer();
}
