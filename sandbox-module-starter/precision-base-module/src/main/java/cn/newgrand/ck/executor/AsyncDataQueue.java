package cn.newgrand.ck.executor;

import org.jctools.queues.MpmcArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * 存储数据且异步自旋的消费数据
 * 异步计算的抽象类，异步的处理添加到其中的所有数据，具有以下特点
 * 1.数据处理是异步的
 * 2.异步处理数据，无数据时消费线程阻塞
 * 3.存储数据的队列大小固定，队列满时数据丢弃，不会造成OOM
 * 4.消费异常次数过多会停止消费，直到被唤醒
 */
public class AsyncDataQueue<T> {

    private final Logger log = LoggerFactory.getLogger(AsyncDataQueue.class);
    private final MpmcArrayQueue<T> dataQueue;
    private final Semaphore semaphore;
    /**
     * @param dataQueueSize 数据队列大小
     *
     */
    public AsyncDataQueue(int dataQueueSize) {
        dataQueue = new MpmcArrayQueue<>(dataQueueSize);
        semaphore = new Semaphore(0);
    }



    /**
     * @param data
     */
    public void put(T data){
        boolean offer = dataQueue.offer(data);
        if(offer){
            semaphore.release();
            return;
        }
        log.warn("队列已满，数据丢弃");
    }

    /**
     * 队列中返回数据，无数据时方法阻塞
     * 异常时返回null
     */
    public T get(){
        try {
            semaphore.acquire();
            return dataQueue.poll();
        } catch (InterruptedException e) {
            log.error("消费中断异常");
        }
        return null;
    }






}
