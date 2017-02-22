package com.motondon.threadpoolexecutordemoapp.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.motondon.threadpoolexecutordemoapp.R;
import com.motondon.threadpoolexecutordemoapp.common.Constants;
import com.motondon.threadpoolexecutordemoapp.service.DummyService;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CurrentTestFragment extends Fragment {

    public static final String TAG = CurrentTestFragment.class.getSimpleName();

    @BindView(R.id.txtEnqueuedTasks) TextView txtEnqueuedTasks;
    @BindView(R.id.txtStartedTasks) TextView txtStartedTasks;
    @BindView(R.id.txtCompletedTasks) TextView txtCompletedTasks;
    @BindView(R.id.txtNotCompletedTasks) TextView txtNotCompletedTasks;
    @BindView(R.id.txtRejectedTasks) TextView txtRejectedTasks;

    @BindView(R.id.txtNumberOfTasks) TextView txtNumberOfTasks;
    @BindView(R.id.txtRejectionMode) TextView txtRejectionMode;
    @BindView(R.id.txtQueueSize) TextView txtQueueSize;
    @BindView(R.id.txtNumberOfActiveThreads) TextView txtNumberOfActiveThreads;
    @BindView(R.id.txtNumberOfThreadsInThePool) TextView txtNumberOfThreadsInThePool;
    @BindView(R.id.seekMaximumThreadPool) SeekBar seekMaximumThreadPool;

    @BindView(R.id.progressBarLoadingImage) ProgressBar progressBarLoadingImage;

    private Integer numberOfRequestedTasks = 0;
    private Integer numberOfEnqueuedTasks = 0;
    private Integer numberOfStartedTasks = 0;
    private Integer numberOfCompletedTasks = 0;
    private Integer numberOfNotCompletedTasks = 0;
    private Integer numberOfRejectedTasks = 0;

    // This will hold a sum of completed, not completed and rejected tasks.
    private Integer numberOfFinalizedTasks = 0;

    private Integer maximumThreadPoolSize = 0;

    private boolean testAlreadyStopped = false;
    private boolean isTestBeingExecuted = false;

    private LocalBroadcastManager mLocalBroadcastManager;

    // DummyService will broadcast messages during its execution. Example of actions are: when a task is finished successfully, rejected, not completed, etc.
    private final BroadcastReceiver mBroadcastReceiver  = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(Constants.TASK_ENQUEUED)) {
                Log.d(TAG, "BroadcastReceiver::onReceive - Received a TASK_ENQUEUED action.");
                Integer currentNumberOfEnqueuedTasks = Integer.parseInt(txtEnqueuedTasks.getText().toString());
                numberOfEnqueuedTasks = ++currentNumberOfEnqueuedTasks;
                txtEnqueuedTasks.setText(numberOfEnqueuedTasks.toString());
            }

            if (intent.getAction().equals(Constants.TASK_REJECTED)) {
                Log.d(TAG, "BroadcastReceiver::onReceive - Received a TASK_REJECTED action.");
                Integer currentNumberOfRejectedTasks = Integer.parseInt(txtRejectedTasks.getText().toString());
                numberOfRejectedTasks = ++currentNumberOfRejectedTasks;
                txtRejectedTasks.setText(numberOfRejectedTasks.toString());

                // Increment general counter when a task is rejected. In the end, it should be exactly equals to the number of requested tasks, no
                // matter how a task was finished (i.e.: completed, not completed or rejected).
                numberOfFinalizedTasks++;

                // Check whether all requested tasks were already finalized. 
                checkWhetherAllTasksWereFinalized();
            }

            if (intent.getAction().equals(Constants.TASK_STARTED)) {
                Log.d(TAG, "BroadcastReceiver::onReceive - Received a TASK_STARTED action. Calling taskStarted() method...");
                taskStarted();
            }

            if (intent.getAction().equals(Constants.TASK_FINISHED)) {
                Log.d(TAG, "BroadcastReceiver::onReceive - Received a TASK_FINISHED action. Calling taskFinished() method...");
                taskFinished();
            }

            if (intent.getAction().equals(Constants.QUEUE_SIZE_CHANGED)) {
                Log.d(TAG, "BroadcastReceiver::onReceive - Received a QUEUE_SIZE_CHANGED action. Updating GUI...");

                Integer currentQueueSize = Integer.parseInt(txtQueueSize.getText().toString());

                currentQueueSize = intent.getIntExtra(Constants.CURRENT_QUEUE_SIZE, currentQueueSize);
                txtQueueSize.setText(currentQueueSize.toString());
            }

            if (intent.getAction().equals(Constants.ACTIVE_THREADS_SIZE_CHANGED)) {
                Log.d(TAG, "BroadcastReceiver::onReceive - Received a ACTIVE_THREADS_SIZE_CHANGED action. Updating GUI...");

                Integer numberOfActiveThreads = Integer.parseInt(txtNumberOfActiveThreads.getText().toString());

                numberOfActiveThreads = intent.getIntExtra(Constants.CURRENT_ACTIVE_THREADS_SIZE, numberOfActiveThreads);
                txtNumberOfActiveThreads.setText(numberOfActiveThreads.toString());
            }

            if (intent.getAction().equals(Constants.NUMBER_OF_THREADS_IN_THE_POOL)) {
                Log.d(TAG, "BroadcastReceiver::onReceive - Received a NUMBER_OF_THREADS_IN_THE_POOL action. Updating GUI...");

                Integer numberOfThreadsInThePool = Integer.parseInt(txtNumberOfThreadsInThePool.getText().toString());

                numberOfThreadsInThePool = intent.getIntExtra(Constants.NUMBER_OF_THREADS, numberOfThreadsInThePool);
                txtNumberOfThreadsInThePool.setText(numberOfThreadsInThePool.toString());
            }

            // This event is received when executor could not be finalized successfully
            if (intent.getAction().equals(Constants.THREAD_POOL_HAS_NOT_FINALIZED)) {
                Log.d(TAG, "BroadcastReceiver::onReceive - Received a THREAD_POOL_HAS_NOT_FINALIZED action.");
                Toast.makeText(getContext(), "Failure when trying to finalize executor.", Toast.LENGTH_SHORT).show();
                resetProgressBar();
            }

            if (intent.getAction().equals(Constants.THREAD_POOL_FINALIZED)) {
                Log.d(TAG, "BroadcastReceiver::onReceive - Received a THREAD_POOL_FINALIZED action.");
                resetProgressBar();
            }

            if (intent.getAction().equals(Constants.THREAD_POOL_FINALIZED_DUE_SHUTDOWN_NOW)) {
                Log.d(TAG, "BroadcastReceiver::onReceive - Received a THREAD_POOL_FINALIZED_DUE_SHUTDOWN_NOW action.");
                resetProgressBar();

                // When user clicks on the StopNow button, executor will call its shutdonwNow() method which, depends the rejection policy,
                // will reject all pending tasks. So, this action will contain extra information. Get all them and show them to the user.
                Bundle data = intent.getExtras();
                numberOfNotCompletedTasks = data.getInt(Constants.NUMBER_OF_NOT_COMPLETED_TASKS);
                Integer currentNumberOfNotCompletedTasks = Integer.parseInt(txtNotCompletedTasks.getText().toString());
                numberOfNotCompletedTasks = currentNumberOfNotCompletedTasks + numberOfNotCompletedTasks;

                // Add to the general counter the number of not completed tasks. In the end, it should be exactly equals to the number of requested tasks, no
                // matter how a task was finished (i.e.: completed, not completed or rejected).
                numberOfFinalizedTasks += numberOfNotCompletedTasks;

                // And check whether all requested tasks were already finalized. 
                checkWhetherAllTasksWereFinalized();

                txtNotCompletedTasks.setText(numberOfNotCompletedTasks.toString());
                Toast.makeText(getContext(), "Thread Pool finalized successfully.", Toast.LENGTH_SHORT).show();
            }

            setHasOptionsMenu(true);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate()");

        super.onCreate(savedInstanceState);

        // Register LocalBroadcastManager actions
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getContext());
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Constants.TASK_STARTED);
        mIntentFilter.addAction(Constants.TASK_ENQUEUED);
        mIntentFilter.addAction(Constants.TASK_REJECTED);
        mIntentFilter.addAction(Constants.TASK_FINISHED);
        mIntentFilter.addAction(Constants.QUEUE_SIZE_CHANGED);
        mIntentFilter.addAction(Constants.ACTIVE_THREADS_SIZE_CHANGED);
        mIntentFilter.addAction(Constants.NUMBER_OF_THREADS_IN_THE_POOL);
        mIntentFilter.addAction(Constants.THREAD_POOL_HAS_NOT_FINALIZED);
        mIntentFilter.addAction(Constants.THREAD_POOL_FINALIZED_DUE_SHUTDOWN_NOW);
        mIntentFilter.addAction(Constants.THREAD_POOL_FINALIZED);
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, mIntentFilter);
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy()");

        super.onDestroy();

        resetProgressBar();

        // Do not forget to unregister the broadcastReceiver when destroying this fragment.
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView()");

        View rootView = inflater.inflate(R.layout.fragment_current_test, container, false);

        ButterKnife.bind(this, rootView);

        // Set the seekMaximumThreadPool max value to 1000.
        maximumThreadPoolSize = getArguments().getInt(Constants.MAXIMUM_THREAD_POOL_SIZE, Integer.MAX_VALUE);
        seekMaximumThreadPool.setMax(1000);
        seekMaximumThreadPool.setProgress(maximumThreadPoolSize);

        final int coreThreadPoolSize = getArguments().getInt(Constants.CORE_THREAD_POOL_SIZE, 0);

        // When user changes the maximum number of threads, send an intent to the DummyService in order for the executor to update the maximum
        // allowed threads size.
        seekMaximumThreadPool.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < coreThreadPoolSize) {
                    Log.i(TAG, "seekMaximumThreadPool::onProgressChanged() - Cannot set maximum thread pool to a value less than core thread pool size (" + coreThreadPoolSize + "). Using [coreThreadPoolSize] instead.");
                    progress = coreThreadPoolSize;
                }

                Log.v(TAG, "seekMaximumThreadPool::onProgressChanged() - New value: " + progress);
                Intent i = new Intent(getContext(), DummyService.class);
                i.setAction(Constants.MAXIMUM_THREAD_POOL_SIZE_CHANGED);
                i.putExtra(Constants.MAXIMUM_THREAD_POOL_SIZE, progress);
                getContext().startService(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        numberOfRequestedTasks = getArguments().getInt(Constants.NUMBER_OF_REQUESTED_TASKS);
        txtNumberOfTasks.setText(numberOfRequestedTasks.toString());

        int rejectionMode = getArguments().getInt(Constants.REJECTION_MODE);
        switch (rejectionMode) {
            case Constants.ABORT_POLICY:
                txtRejectionMode.setText("abortPolicy");
                break;

            case Constants.CALLER_RUNS_POLICY:
                txtRejectionMode.setText("callerRunsPolicy");
                break;

            case Constants.CUSTOM_REJECTION_POLICY:
                txtRejectionMode.setText("customRejectionPolicy");
                break;
        }

        // If user set preStartCoreThreads check-box, Executor should create the core threads ahead. Then, get it
        // and update the GUI.
        Integer numberOfThreadsInThePool = getArguments().getInt(Constants.NUMBER_OF_THREADS_IN_THE_POOL, 0);
        txtNumberOfThreadsInThePool.setText(numberOfThreadsInThePool.toString());

        // Prevent keyboard to appear when opening this fragment
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        progressBarLoadingImage.setMax(numberOfRequestedTasks);

        return rootView;
    }

    @OnClick(R.id.btnStart)
    public void onStartClick() {
        Log.v(TAG, "onStartClick()");

        if (isTestBeingExecuted) {
            Toast.makeText(getContext(), "Detected a test is being executed. Please, either wait until it finishes or stop it prior to start a new one.", Toast.LENGTH_LONG).show();
            return;
        }

        numberOfFinalizedTasks = 0;
        numberOfRejectedTasks = 0;
        numberOfCompletedTasks = 0;
        numberOfEnqueuedTasks = 0;
        numberOfNotCompletedTasks = 0;
        numberOfStartedTasks = 0;

        seekMaximumThreadPool.setProgress(maximumThreadPoolSize);
        progressBarLoadingImage.setMax(numberOfRequestedTasks);
        txtEnqueuedTasks.setText("0");
        txtStartedTasks.setText("0");
        txtCompletedTasks.setText("0");
        txtNotCompletedTasks.setText("0");
        txtRejectedTasks.setText("0");

        isTestBeingExecuted = true;

        resetProgressBar();

        // Call the DummyService by passing START action. This will start the executor.
        Intent i = new Intent(getContext(), DummyService.class);
        i.setAction(Constants.START);
        i.putExtra(Constants.NUMBER_OF_TASKS, numberOfRequestedTasks);

        Log.d(TAG, "onStartClick() - Calling DummyService with action START. Number of tasks: " + numberOfRequestedTasks);
        getContext().startService(i);
    }

    private void resetProgressBar() {
        progressBarLoadingImage.setProgress(0);
    }

    @OnClick(R.id.btnStop)
    public void onStopClick() {
        Log.d(TAG, "onStopClick() - Calling DummyService with action STOP.");

        // Call DummyService by passing STOP action. This will stop the executor smoothly.
        Intent i = new Intent(getContext(), DummyService.class);
        i.setAction(Constants.STOP);
        getContext().startService(i);

        testAlreadyStopped = true;
        isTestBeingExecuted = false;

        progressBarLoadingImage.setProgress(0);
        Toast.makeText(getContext(), "Test stopped successfully. Wait for the enqueued tasks to finish...", Toast.LENGTH_LONG).show();
    }

    @OnClick(R.id.btnStopNow)
    public void onStopNowClick() {
        Log.d(TAG, "onStopNowClick() - Calling DummyService with action STOP_NOW.");

        // Call DummyService by passing STOP_NOW action. This will make executor to call its shutdownNow() method.
        Intent i = new Intent(getContext(), DummyService.class);
        i.setAction(Constants.STOP_NOW);
        getContext().startService(i);

        testAlreadyStopped = true;
        isTestBeingExecuted = false;

        progressBarLoadingImage.setProgress(0);


        Toast.makeText(getContext(), "Test stopped successfully.", Toast.LENGTH_LONG).show();
    }

    /**
     * Whenever a task is started, this event will be received.
     */
    private void taskStarted() {
        Log.d(TAG, "taskStarted()");
        txtStartedTasks.setText((++numberOfStartedTasks).toString());
    }

    /**
     * Whenever a task is finished, this event will be received. Note that if we need a more robust monitor, we should use a Callable instead of a
     * Runnable as the base class of SleekTask. This would give us a Future object that we could better monitor tasks status.
     *
     */
    private void taskFinished() {
        Log.d(TAG, "taskFinished()");

        ++numberOfCompletedTasks;
        
        // Increment general counter when a task is finished. In the end, it should be exactly equals to the number of requested tasks, no
        // matter how a task was finished (i.e.: completed, not completed or rejected).        
        ++numberOfFinalizedTasks;

        txtCompletedTasks.setText(numberOfCompletedTasks.toString());

        // And check whether all requested tasks were already finalized.
        checkWhetherAllTasksWereFinalized();
    }

    /**
     * This method is called whenever a task is finalized as completed, not completed or rejected. It checks whether all requested tasks were finalized.
     *
     */
    private void checkWhetherAllTasksWereFinalized() {

        if (!isTestBeingExecuted) {
            // If a test is stopped by pressing either stop or stopNow buttons, there is nothing to do here.
            Log.d(TAG, "checkWhetherAllTasksWereFinalized() - Detected test already stopped. Nothing to do here.");
            return;
        }

        Log.v(TAG, "checkWhetherAllTasksWereFinalized() - numberOfFinalizedTasks: " + numberOfFinalizedTasks + " - numberOfRequestedTasks: " + numberOfRequestedTasks);

        if (numberOfFinalizedTasks.equals(numberOfRequestedTasks)) {
            Log.d(TAG, "checkWhetherAllTasksWereFinalized() - Detected all requested tasks have been processed. Reset progress bar.");
            progressBarLoadingImage.setProgress(0);
            isTestBeingExecuted = false;

            if (testAlreadyStopped) {
                Toast.makeText(getContext(), "Test finished. Detected executor was previously shutdown. ", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Test finished successfully. Number of threads in the pool will decrease until it hits the core pool size.", Toast.LENGTH_LONG).show();
            }
        } else {
            progressBarLoadingImage.setProgress(numberOfFinalizedTasks);
        }
    }


}
