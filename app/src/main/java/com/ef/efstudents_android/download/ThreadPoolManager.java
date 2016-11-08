package com.ef.efstudents_android.download;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * 对线程池进行管理和封装
 *
 * @author Administrator
 */
public class ThreadPoolManager {
    private static ThreadPoolManager mInstance = new ThreadPoolManager();
    private ThreadPoolExecutor executor;

    private int corePoolSize;//核心线程池数量，表示能够同时执行的任务数量
    private int maximumPoolSize;//最大线程池数量，其实是包含了核心线程池数量在内的
    private long keepAliveTime = 1;//存活时间,表示最大线程池中等待任务的存活时间
    private TimeUnit unit = TimeUnit.HOURS;//存活时间的时间单位

    public static ThreadPoolManager getInstance() {
        return mInstance;
    }

    private ThreadPoolManager() {
        //核心线程池数量的计算规则：当前设备的可用处理器核心数*2 + 1,能够让CPU得到最大效率的发挥；
        corePoolSize = Runtime.getRuntime().availableProcessors() * 2 + 1;
        maximumPoolSize = corePoolSize;//虽然用不到，但是不能是0，否则报错
        executor = new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,//当缓冲队列满的时候会放入最大线程池等待
                keepAliveTime,
                unit,
                new LinkedBlockingQueue<Runnable>(),//缓冲队列,超出核心线程池的任务会被放入缓存队列中等待
                Executors.defaultThreadFactory(),//创建线程的工厂类
                new ThreadPoolExecutor.AbortPolicy()//当最大线程池也超出的时候，则拒绝执行
        );
    }

    /**
     * 往线程池中添加任务
     *
     * @param r
     */
    public void execute(Runnable r) {
        if (r != null) {
            executor.execute(r);
        }
    }

    /**
     * 从线程池中移除任务
     *
     * @param r
     */
    public void remove(Runnable r) {
        if (r != null) {
            executor.remove(r);
        }
    }

    /**
     * 从线程池中移除任务
     *
     */
    public BlockingQueue<Runnable> getQueue() {
        return executor.getQueue();
    }


}
