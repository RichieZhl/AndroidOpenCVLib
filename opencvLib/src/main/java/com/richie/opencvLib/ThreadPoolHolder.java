package com.richie.opencvLib;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by cht on 2018/3/7.
 */

public class ThreadPoolHolder {

    private static final int CORE_SIZE = Runtime.getRuntime().availableProcessors();
    private final ThreadPoolExecutor mThreadPoolExecutor;
    private Handler sMainHandler;

    private static final class SInstanceHolder {
        static final ThreadPoolHolder sInstance = new ThreadPoolHolder();
    }

    public static ThreadPoolHolder getInstance() {
        return SInstanceHolder.sInstance;
    }

    private ThreadPoolHolder() {
        mThreadPoolExecutor = new ThreadPoolExecutor(
                CORE_SIZE,
                CORE_SIZE * 2,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(),
                Executors.defaultThreadFactory()
        );
    }

    public void execute(Runnable runnable) {
        mThreadPoolExecutor.execute(runnable);
    }

    public boolean isTerminated() {
        return mThreadPoolExecutor.isTerminated();
    }

    public void shutdown() {
        mThreadPoolExecutor.shutdown();
    }


    public void runOnUiThread(Runnable runnable) {
        synchronized (ThreadPoolHolder.class) {
            if (sMainHandler == null) {
                sMainHandler = new Handler(Looper.getMainLooper());
            }
        }
        sMainHandler.post(runnable);
    }

    public void runOnUiThreadDelay(Runnable runnable, long delayMillis) {
        synchronized (ThreadPoolHolder.class) {
            if (sMainHandler == null) {
                sMainHandler = new Handler(Looper.getMainLooper());
            }
        }
        sMainHandler.postDelayed(runnable, delayMillis);
    }
}
