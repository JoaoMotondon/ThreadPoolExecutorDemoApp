package com.motondon.threadpoolexecutordemoapp.task;

import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This is a custom ThreadPoolExecutor. 
 * 
 * The reason we created it is that we need to hook when a task is started (by overriding beforeExecute) and when it is finished
 * (by overriding afterExecute). Both methods will get active thread count and the queue size. 
 *
 */
public class CustomThreadPoolExecutor extends ThreadPoolExecutor {

    private static final String TAG = CustomThreadPoolExecutor.class.getSimpleName();

    public interface ThreadPoolExecutorListener {
        void activeThreadCountChanged(int activeCount);
        void queueSizeChanged(int queueSize);
    }

    ThreadPoolExecutorListener threadPoolExecutorListener;

    public CustomThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadPoolExecutorListener threadPoolExecutorListener) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        this.threadPoolExecutorListener = threadPoolExecutorListener;
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);

        int activeCount = getActiveCount();
        int queueSize = getQueue().size();
        Log.d(TAG, "beforeExecute() - activeThreadCountChanged: " + activeCount + " - queueSizeChanged: " + queueSize);
        threadPoolExecutorListener.activeThreadCountChanged(activeCount);
        threadPoolExecutorListener.queueSizeChanged(queueSize);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);

        int activeCount = getActiveCount();
        int queueSize = getQueue().size();
        Log.d(TAG, "afterExecute() - activeThreadCountChanged: " + activeCount + " - queueSizeChanged: " + queueSize);
        threadPoolExecutorListener.activeThreadCountChanged(activeCount);
        threadPoolExecutorListener.queueSizeChanged(queueSize);
    }

    @Override
    protected void terminated() {
        super.terminated();

        Log.d(TAG, "terminated()");
    }
}
