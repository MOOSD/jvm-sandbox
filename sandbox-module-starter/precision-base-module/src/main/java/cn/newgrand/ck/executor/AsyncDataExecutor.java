package cn.newgrand.ck.executor;

import org.jctools.queues.MpmcArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.*;

/**
 * 存储数据且异步自旋的消费数据
 * 异步计算的抽象类，异步的处理添加到其中的所有数据，具有以下特点
 * 1.数据处理是异步的
 * 2.异步处理数据，无数据时消费线程阻塞
 * 3.存储数据的队列大小固定，队列满时数据丢弃，不会造成OOM
 */
public class AsyncDataExecutor<T> {

    private final Logger log = LoggerFactory.getLogger(AsyncDataExecutor.class);
    private final MpmcArrayQueue<T> dataQueue;

    private final ThreadPoolExecutor consumerThreadPool;

    private final ConsumerBuilder<T> consumerBuilder;

    private final Semaphore semaphore;
    /**
     * @param dataQueueSize 数据队列大小
     * @param consumerCount 消费者数量
     */
    public AsyncDataExecutor(int dataQueueSize, int consumerCount, ConsumerBuilder<T> consumerBuilder) {
        this.consumerBuilder = consumerBuilder;
        dataQueue = new MpmcArrayQueue<>(dataQueueSize);

        consumerThreadPool = new ThreadPoolExecutor(consumerCount, consumerCount,
                0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>());

        semaphore = new Semaphore(0);
    }

    /**
     * 激活数据处理和消费
     */
    public void active(){
        // 创建消费线程
        for (int i = 0; i < consumerThreadPool.getMaximumPoolSize(); i++) {
            CompletableFuture.runAsync(getConsumerRunnable(), consumerThreadPool).exceptionally(throwable -> {
                log.error("消费任务异常",throwable);
                return null;
            });
        }
    }

    /**
     * @param data
     */
    public void put(T data){
        dataQueue.offer(data);
        semaphore.release();

    }

    public Runnable getConsumerRunnable(){
        // 实例化consumer
        DataConsumer<T> consumer = consumerBuilder.getConsumer();
        return () -> {
            Object param;
            while(true){
                // 异常不终止消费行为
                try{
                    semaphore.acquire();
                    T data = dataQueue.poll();
                    if (Objects.nonNull(data)){
                        consumer.comsumer(data);
                    }
                }catch (InterruptedException exception){
                    log.error("消费中断异常");
                }
                catch (Throwable exception){
                    log.error("数据消费异常",exception);
                }
            }

        };
    }




}
