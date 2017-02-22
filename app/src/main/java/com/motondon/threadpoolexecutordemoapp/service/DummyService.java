package com.motondon.threadpoolexecutordemoapp.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.motondon.threadpoolexecutordemoapp.common.Constants;
import com.motondon.threadpoolexecutordemoapp.task.CustomThreadPoolExecutor;
import com.motondon.threadpoolexecutordemoapp.task.SleepTask;
import com.motondon.threadpoolexecutordemoapp.view.CurrentTestFragment;
import com.motondon.threadpoolexecutordemoapp.view.MainFragment;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This is the heart of this app. 
 * 
 * This Service will be created when an user clicks on the "Initialize Pool" button in the MainFragment. Then it will
 * create and initialize the executor but will not start it. Only when user clicks on the "start"  button (in the 
 * CurrentTestFragment), executor will starts.
 * 
 * During the executor execution, as tasks are being started, finalized, rejected, events will be sent to the CurrentTestFragment
 * via LocalBroadcast.
 * 
 */
public class DummyService extends Service implements SleepTask.ThreadCallback, CustomThreadPoolExecutor.ThreadPoolExecutorListener {

	private static final String TAG = DummyService.class.getSimpleName();

    // LocalBbroadcast which will allow this service to send messages to the fragments.
	private LocalBroadcastManager mLocalBroadcastManager;

	// This is the custom ThreadPoolExecutor.
	private CustomThreadPoolExecutor executor;

	private int numberOfTasks = 0;

	// This timer will be scheduled every one second in order to get the number of the threads in the pool.  
    Timer threadsPoolSizeTimer = new Timer();

	@Override
	public void onCreate() {
		Log.i(TAG, "onCreate");
		super.onCreate();

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.i(TAG, "onBind");

		// Since we are not dealing with a bounded service, just return null.
		return null;
	}

	/**
     * This is the entry point for this service. It will accept different commands: INIT, START, 
     * STOP, etc, in order to manipulate the Executor.
	 *
	 * @param intent
	 * @param flags
	 * @param startId
	 * @return
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		String action = intent.getAction();

		switch (action) {
		case Constants.INIT:
			Log.i(TAG, "onStartCommand - Init");
			onInit(intent);
			break;

		case Constants.START:
			Log.i(TAG, "onStartCommand - Start");
			onStart();
			break;

		// This command will be sent when user changes maximum thread pool size while a test is running. 
		case Constants.MAXIMUM_THREAD_POOL_SIZE_CHANGED:
			int maximumThreadPool = intent.getIntExtra(Constants.MAXIMUM_THREAD_POOL_SIZE, Integer.MAX_VALUE);
			Log.i(TAG, "onStartCommand - MAXIMUM_THREAD_POOL_SIZE_CHANGED - New value: " + maximumThreadPool);
			executor.setMaximumPoolSize(maximumThreadPool);
			break;

		case Constants.STOP:
			Log.i(TAG, "onStartCommand - Stop");
			onStop();
			break;

		case Constants.STOP_NOW:
			Log.i(TAG, "onStartCommand - Stop Now");
			onStopNow();
			break;

        case Constants.DESTROY:
            Log.i(TAG, "onStartCommand - Destroy service");
            onDestroyService();
            break;
		}

		return START_STICKY;
	}

    /**
     * Initialize the executor based on the user settings (number of core threads, queue size, etc)
	 * 
	 * @param intent
	 */
	private void onInit(Intent intent) {
		Log.v(TAG, "onInit - Begin");

		try {

			Bundle data = intent.getExtras();
			int rejection_mode = data.getInt(Constants.REJECTION_MODE);

			// This will be used later when starting this test
			numberOfTasks = data.getInt(Constants.NUMBER_OF_TASKS);

            int coreThreadPoolSize = data.getInt(Constants.CORE_THREAD_POOL_SIZE);
            int maximumThreadPoolSize  = data.getInt(Constants.MAXIMUM_THREAD_POOL_SIZE);
			boolean preStartCoreThreads = data.getBoolean(Constants.PRE_START_CORE_THREADS);
			int queueSize = data.getInt(Constants.QUEUE_SIZE);
			Log.d(TAG, "onInit - rejection_mode: " + rejection_mode);
			Log.d(TAG, "onInit - Number of tasks: " + numberOfTasks);
            Log.d(TAG, "onInit - coreThreadPoolSize: " + coreThreadPoolSize);
            Log.d(TAG, "onInit - maximumThreadPoolSize: " + maximumThreadPoolSize);
			Log.d(TAG, "onInit - maximumThreadPoolSize: " + maximumThreadPoolSize);
			Log.d(TAG, "onInit - preStartCoreThreads: " + preStartCoreThreads);
			Log.d(TAG, "onInit - queueSize: " + queueSize);

			executor = new CustomThreadPoolExecutor(
					coreThreadPoolSize,
					maximumThreadPoolSize,
					5L,
					TimeUnit.SECONDS,
					new LinkedBlockingQueue<Runnable>(queueSize), this);

			executor.setMaximumPoolSize(maximumThreadPoolSize);
			executor.setCorePoolSize(coreThreadPoolSize);

			if (preStartCoreThreads) {
				Log.d(TAG, "onInit() - Requesting executor to pre start its core threads...");
				int x = executor.prestartAllCoreThreads();
				Log.d(TAG, "onInit() - Executor started " + x + " core threads");
			}

            switch (rejection_mode) {
                case Constants.ABORT_POLICY:
                        executor.setRejectedExecutionHandler(new CustomThreadPoolExecutor.AbortPolicy());
                    break;

                case Constants.CALLER_RUNS_POLICY:
                        executor.setRejectedExecutionHandler(new CustomThreadPoolExecutor.CallerRunsPolicy());
                    break;

                case Constants.CUSTOM_REJECTION_POLICY:
                    // TODO: Create an instance of our custom rejection policy
                    executor.setRejectedExecutionHandler(new RejectedExecutionHandler() {
						@Override
						public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
							throw new RejectedExecutionException("CustomRejectionPolicy - Task " + r.toString() +
									" rejected from " +
									executor.toString());
						}
					});
                    break;
            }

			Log.d(TAG, "onInit() - Sending to the  " + MainFragment.class.getSimpleName() + " THREAD_POOL_INITIALIZED action.");

            // Now, reuse received intent in order to reuse its extras
            intent.setAction(Constants.THREAD_POOL_INITIALIZED);

            if (preStartCoreThreads) {

                // Now, get the number of threads in the pool, just to be sure prestartAllCoreThreads worked as expected
                int poolSize = executor.getPoolSize();
				Log.d(TAG, "onInit() - Detected number of threads in the pool is: " + poolSize);
                intent.putExtra(Constants.NUMBER_OF_THREADS_IN_THE_POOL, poolSize);
			}

            // Create a timer which will get the number of threads in the pool every one second.
            createThreadsPoolSizeTimer();

            // Finally send this intent. It will be handled by the MainFragment which will then start CurrentTestFragment.
			mLocalBroadcastManager.sendBroadcast(intent);

		} catch (Exception e) {

            Log.e(TAG, "onInit() - Sending to the  " + MainFragment.class.getSimpleName() + " THREAD_POOL_INITIALIZATION_FAILED action: " + e.getMessage());
			Intent i = new Intent(Constants.THREAD_POOL_INITIALIZATION_FAILED);
			mLocalBroadcastManager.sendBroadcast(i);
		}

		Log.v(TAG, "onInit - End");
	}

	/**
	 * This event is fired when CustomThreadPoolExecutor detects the queue size is changed.
	 *
	 * @param queueSize
     */
	@Override
	public void queueSizeChanged(int queueSize) {
		Log.d(TAG, "queueSizeChanged() - Sending to the  " + CurrentTestFragment.class.getSimpleName() + " QUEUE_SIZE_CHANGED action. Number: " + queueSize);
		Intent i = new Intent(Constants.QUEUE_SIZE_CHANGED);
		i.putExtra(Constants.CURRENT_QUEUE_SIZE, queueSize);
		mLocalBroadcastManager.sendBroadcast(i);
	}

	/**
	 * This event is fired when CustomThreadPoolExecutor detects the active thread count is changed.
	 *
	 * @param activeCount
     */
	@Override
	public void activeThreadCountChanged(int activeCount) {
		Log.d(TAG, "activeThreadCountChanged() - Sending to the  " + CurrentTestFragment.class.getSimpleName() + " ACTIVE_THREADS_SIZE_CHANGED action. Number: " + activeCount);
		Intent i = new Intent(Constants.ACTIVE_THREADS_SIZE_CHANGED);
		i.putExtra(Constants.CURRENT_ACTIVE_THREADS_SIZE, activeCount);
		mLocalBroadcastManager.sendBroadcast(i);
	}

	/**
     * Start the CustomThreadPoolExecutor. Since a Service by default runs in the main UI thread, it will start the executor in a worker 
     * thread (by calling Executors.newSingleThreadExecutor).
     * 
     * It uses SleepTask runnable in order to simulate a background process. If for some reason a task cannot be created, 
     * depends on the rejection policy, a RejectedExecutiongException will be thrown. 
	 *
	 */
	private void onStart() {
		Log.v(TAG, "onStart - Begin");

		Log.d(TAG, "onStart() - Number of tasks: " + numberOfTasks);

		// Run it in a separate thread in order to prevent GUI to freeze.
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {

				for (int x = 0; x < numberOfTasks; x++) {

					try {
						Log.d(TAG, "onStart() - Creating task (" + x + ")...");
						executor.execute(new SleepTask(DummyService.this));
                        Log.d(TAG, "onStart() - Sending to the  " + CurrentTestFragment.class.getSimpleName() + " TASK_ENQUEUED action.");
						Intent i = new Intent(Constants.TASK_ENQUEUED);
						mLocalBroadcastManager.sendBroadcast(i);

						Thread.sleep(10);

					} catch (RejectedExecutionException e) {
                        Log.w(TAG, "onStart() - Task rejected. Sending to the  " + CurrentTestFragment.class.getSimpleName() + " TASK_REJECTED action. Message: " + e.getMessage());
						Intent i = new Intent(Constants.TASK_REJECTED);
						mLocalBroadcastManager.sendBroadcast(i);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});

		Log.v(TAG, "onStart() - End");
	}

	/**
     * Call Executor.shutdown() which will prevent new tasks to be queued, but all those already enqueued will still be processed accordingly.
	 *
     * If the shutdown process takes more than 5 seconds, a call shutdownNow() will be made to force the shutdown process.
	 *
	 */
	void onStop() {
		Log.v(TAG, "onStop() - Begin");

		executor.shutdown(); // Disable new tasks from being submitted

		// Run it in a separate thread in order to prevent GUI to freeze.
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				try {

					// Wait a while for existing tasks to terminate
					if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                        Log.w(TAG, "onStop() - Shutdown process did not finish in 10 seconds. Calling shutdownNow()...");
                        executor.shutdownNow(); // Cancel currently executing tasks

						// Wait a while for tasks to respond that they are being cancelled
						if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                            Log.w(TAG, "onStop() -  ShutdownNow process did not finish in 10 seconds. Sending to the  " + CurrentTestFragment.class.getSimpleName() + " THREAD_POOL_HAS_NOT_FINALIZED action.");
                            Intent i = new Intent(Constants.THREAD_POOL_HAS_NOT_FINALIZED);
							mLocalBroadcastManager.sendBroadcast(i);

						} else {
                            Log.d(TAG, "onStop() - Sending to the  " + CurrentTestFragment.class.getSimpleName() + " THREAD_POOL_FINALIZED action.");
                            Intent i = new Intent(Constants.THREAD_POOL_FINALIZED);
							mLocalBroadcastManager.sendBroadcast(i);
						}

					} else {

                        Log.d(TAG, "onStop() - shutdown process finished accordingly. Sending to the  " + CurrentTestFragment.class.getSimpleName() + " THREAD_POOL_FINALIZED action.");
						Intent i = new Intent(Constants.THREAD_POOL_FINALIZED);
						mLocalBroadcastManager.sendBroadcast(i);

					}
				} catch (InterruptedException ie) {
                    Log.w(TAG, "onStop() - InterruptedException fired. Calling shutdownNow()...");

					// (Re-)Cancel if current thread also interrupted
					executor.shutdownNow();

                    Log.w(TAG, "onStop() - Sending to the  " + CurrentTestFragment.class.getSimpleName() + " THREAD_POOL_FINALIZED action.");
					Intent i = new Intent(Constants.THREAD_POOL_FINALIZED);
					mLocalBroadcastManager.sendBroadcast(i);

					// Preserve interrupt status
					Thread.currentThread().interrupt();
				}
			}
		});

		Log.v(TAG, "onStop - End");
	}

	/**
     * Call Executor.shutdownNow() which will prevent new tasks to be queued, as well as reject all enqueued (but not yet processed) tasks.
	 *
     * If the shutdownNow process takes more than 5 seconds, we will assume that an error has occurred.
	 *
     * This method was based on the following link:
     * http://developer.android.com/intl/pt-br/reference/java/util/concurrent/ExecutorService.html
	 *
	 */
	private void onStopNow() {
		Log.v(TAG, "onStopNow() - Begin");

		List<Runnable> notCompletedTasks = executor.shutdownNow();
		Integer numberOfNotCompletedTasks = notCompletedTasks.size();

		try {

			// Wait a while for tasks to respond to being cancelled
			if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                Log.d(TAG, "onStopNow() - ShutdownNow process did not finish in 10 seconds. Sending to the  " + CurrentTestFragment.class.getSimpleName() + " THREAD_POOL_HAS_NOT_FINALIZED action.");
				Intent i = new Intent(Constants.THREAD_POOL_HAS_NOT_FINALIZED);
				mLocalBroadcastManager.sendBroadcast(i);

			} else {
                Log.d(TAG, "onStopNow() - ShutdownNow process finished accordingly. Sending to the  " + CurrentTestFragment.class.getSimpleName() + " THREAD_POOL_FINALIZED action. Number of not completed tasks: " + numberOfNotCompletedTasks);
                Intent i = new Intent(Constants.THREAD_POOL_FINALIZED_DUE_SHUTDOWN_NOW);
                i.putExtra(Constants.NUMBER_OF_NOT_COMPLETED_TASKS, numberOfNotCompletedTasks);
				mLocalBroadcastManager.sendBroadcast(i);
			}

		} catch (InterruptedException ie) {
            Log.w(TAG, "onStopNow() - InterruptedException fired. Calling shutdownNow() again...");

			// (Re-)Cancel if current thread also interrupted
			notCompletedTasks = executor.shutdownNow();
			numberOfNotCompletedTasks = notCompletedTasks.size();

            Log.d(TAG, "onStopNow() - Sending to the  " + CurrentTestFragment.class.getSimpleName() + " THREAD_POOL_FINALIZED action. Number of not completed tasks: " + numberOfNotCompletedTasks);
            Intent i = new Intent(Constants.THREAD_POOL_FINALIZED_DUE_SHUTDOWN_NOW);
            i.putExtra(Constants.NUMBER_OF_NOT_COMPLETED_TASKS, notCompletedTasks.size());
			mLocalBroadcastManager.sendBroadcast(i);

			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}

		Log.v(TAG, "onStopNow() - End");
	}

	/**
	 * When user press UP menu item, we need first to stop any pending tasks and destroy the executor by calling stopSelf method. 
	 */
    private void onDestroyService() {
        Log.i(TAG, "onDestroyService");
        onStopNow();

        stopSelf();
    }

    @Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy");

		super.onDestroy();

		// Do not forget to cancel this timer when destroying the service.
        if (threadsPoolSizeTimer != null) {
            threadsPoolSizeTimer.cancel();
        }
	}

	/**
     * This is a callback method fired by the SleepTask when it is about to start a task execution. 
	 *
	 */
	@Override
	public void taskStarted() {
        Log.d(TAG, "taskStarted() - Sending to the  " + CurrentTestFragment.class.getSimpleName() + " TASK_STARTED action.");
		Intent i = new Intent(Constants.TASK_STARTED);
		mLocalBroadcastManager.sendBroadcast(i);
	}

	/**
     * This is a callback method fired by the SleepTask after it finishes a task execution.
	 *
	 */
	@Override
	public void taskFinished() {
        Log.d(TAG, "taskFinished() - Sending to the  " + CurrentTestFragment.class.getSimpleName() + " TASK_FINISHED action.");
		Intent i = new Intent(Constants.TASK_FINISHED);
		mLocalBroadcastManager.sendBroadcast(i);
	}

	/**
	 * This method creates a timer which will get the number of threads in the pool every one second.
	 */
    private void createThreadsPoolSizeTimer() {

        threadsPoolSizeTimer = new Timer();
        threadsPoolSizeTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                int numberOfThreadsInThePool = executor.getPoolSize();

                // Log.d(TAG, "createThreadsPoolSizeTimer() - Sending to the  " + CurrentTestFragment.class.getSimpleName() + " NUMBER_OF_THREADS_IN_THE_POOL action. Number: " + numberOfThreadsInThePool);
                Intent i = new Intent(Constants.NUMBER_OF_THREADS_IN_THE_POOL);
                i.putExtra(Constants.NUMBER_OF_THREADS, numberOfThreadsInThePool);
                mLocalBroadcastManager.sendBroadcast(i);
            }
        }, 10, 1000);
    }
}
	
