package com.motondon.threadpoolexecutordemoapp.task;

import android.util.Log;

/**
 * This task is intended to only sleep a while in order to simulate a background process.
 *
 */
public class SleepTask implements Runnable {

    private static final String TAG = SleepTask.class.getSimpleName();

    public interface ThreadCallback {
        void taskStarted();
        void taskFinished();
    }

    private ThreadCallback mCallback;

    public SleepTask(ThreadCallback callback) {
        Log.v(TAG, "Constructor");

        this.mCallback = callback;
    }

    @Override
    public void run() {
        Log.v(TAG, "run() - Begin");

        try {

            mCallback.taskStarted();

            long timeToSleep = (long)(Math.random() * ((10000 - 5000) + 1));
            Log.d(TAG, "run() - Sleeping for " +  timeToSleep + " milliseconds...");

            Thread.sleep(timeToSleep);

            Log.d(TAG, "run() - Task finished");

            mCallback.taskFinished();

        } catch (InterruptedException e) {
            Log.e(TAG, "run() - InterruptedException while trying to execute a task. Message: " + e.getMessage());
        }

        Log.v(TAG, "run() - End");
    }
}
