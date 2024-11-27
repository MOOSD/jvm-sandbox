package cn.newgrand.ck.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异步带有缓冲的数据处理器
 * @param <T> 模块收集到的要处理的数据
 */
public class DataProcessor<T> {
    private final Logger log = LoggerFactory.getLogger(DataProcessor.class);

    private final ThreadPoolExecutor consumerThreadPool;

    private final AsyncDataQueue<T> queue;

    protected AtomicInteger dataCount;

    private final int consumerCount;

    private final DataConsumer<T> dataConsumer;

    private boolean isEnable =false;
    private final List<CompletableFuture<Void>> featureList;


    /**
     * @param consumerCount 处理器实例数量
     * @param dataQueueSize 数据队列大小
     */
    public DataProcessor(int consumerCount, int dataQueueSize, DataConsumer<T> consumer) {
        this.consumerCount = consumerCount;
        featureList = new ArrayList<>(consumerCount);
        this.dataCount = new AtomicInteger(0);
        consumerThreadPool = new ThreadPoolExecutor(consumerCount, consumerCount,
                0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>());

        this.queue = new AsyncDataQueue<T>(dataQueueSize);
        // 实例化consumer
        this.dataConsumer = consumer;
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
    public void enable(){
        log.info("数据处理器激活");
        // 已经启动则忽略
        if (isEnable){
            return;
        }

        // 创建消费线程
        for (int i = 0; i < consumerCount; i++) {
            CompletableFuture<Void> exceptionally = CompletableFuture.runAsync(whileTureDataConsumeRunnable(dataConsumer), consumerThreadPool)
                    .exceptionally(throwable -> {
                        log.error("数据处理线程异常：", throwable);
                        return null;
                    });
            featureList.add(exceptionally);
        }

        isEnable = true;
    }

    /**
     * 终止消费行为
     */
    public void disable(){
        log.info("数据处理器关闭");
        // 已停止则忽略
        if (!isEnable){
            return;
        }
        // 停止所有的消费任务
        featureList.forEach(future ->{
            // 此consumer未曾执行
            if (future == null){
                return;
            }
            future.cancel(true);
        });
        featureList.clear();
        isEnable = false;

    }

    /**
     * 生成一个持续进行数据处理的处理器
     */
    public Runnable whileTureDataConsumeRunnable(DataConsumer<T> consumer){
        if (Objects.isNull(consumer)){
            return null;
        }
        return () -> {
            // 除非线程中断，否则不结束消费
            while (!Thread.currentThread().isInterrupted()) {
                // 异常不终止消费行为
                try {
                    T data = queue.get();
                    if (Objects.nonNull(data)) {
                        consumer.consume(data);
                        dataCount.incrementAndGet();
                    }
                } catch (Throwable exception) {
                    log.error("数据消费异常", exception);
                }
            }
        };
    }



}
