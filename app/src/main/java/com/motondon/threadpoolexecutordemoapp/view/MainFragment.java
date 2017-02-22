package com.motondon.threadpoolexecutordemoapp.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.motondon.threadpoolexecutordemoapp.R;
import com.motondon.threadpoolexecutordemoapp.common.Constants;
import com.motondon.threadpoolexecutordemoapp.service.DummyService;
import com.motondon.threadpoolexecutordemoapp.util.InputFilterMinMax;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainFragment extends Fragment {

    public static final String TAG = MainFragment.class.getSimpleName();

    @BindView(R.id.etNumberOfTasks) EditText etNumberOfTasks;
    @BindView(R.id.etCoreThreadPoolSize) EditText etCoreThreadPoolSize;
    @BindView(R.id.etMaximumThreadPoolSize) EditText etMaximumThreadPoolSize;
    @BindView(R.id.etQueueSize) EditText etQueueSize;
    @BindView(R.id.spRejectionMode) Spinner spRejectionMode;
    @BindView(R.id.cbPreStartCoreThreads) CheckBox cbPreStartCoreThreads;

    private LocalBroadcastManager mLocalBroadcastManager;

    // Broadcasts that will receive events from DummyService Service when it is initialized successfully or in case of errors during its 
    // initialization. If success, CurrentTestActivity will be started.
    private final BroadcastReceiver mBroadcastReceiver  = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(Constants.THREAD_POOL_INITIALIZED)) {
                Log.d(TAG, "BroadcastReceiver::onReceive - Received a THREAD_POOL_INITIALIZED action. Starting CurrentTestActivity...");

                intent.setClass(getContext(), CurrentTestActivity.class);
                intent.putExtra(Constants.NUMBER_OF_REQUESTED_TASKS, Integer.parseInt(etNumberOfTasks.getText().toString()));
                getContext().startActivity(intent);
            }

            if (intent.getAction().equals(Constants.THREAD_POOL_INITIALIZATION_FAILED)) {
                Log.w(TAG, "BroadcastReceiver::onReceive - Received a THREAD_POOL_INITIALIZATION_FAILED action.");
                Toast.makeText(getContext(), "Thread Pool NOT initialized. See logs for details.", Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");

        super.onCreate(savedInstanceState);

        // Start listen for THREAD_POOL_INITIALIZED and THREAD_POOL_INITIALIZATION_FAILED actions. They will be sent when Dummy server is initialized.
        Log.d(TAG, "onCreate - initializing mLocalBroadcastManager...");
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getContext());
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Constants.THREAD_POOL_INITIALIZED);
        mIntentFilter.addAction(Constants.THREAD_POOL_INITIALIZATION_FAILED);
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, mIntentFilter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView");

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        ButterKnife.bind(this, rootView);

        etNumberOfTasks.setFilters(new InputFilter[]{new InputFilterMinMax("1", "5000")});
        etCoreThreadPoolSize.setFilters(new InputFilter[]{new InputFilterMinMax("0", String.valueOf(Integer.MAX_VALUE))});
        etMaximumThreadPoolSize.setFilters(new InputFilter[]{new InputFilterMinMax("1", String.valueOf(Integer.MAX_VALUE))});
        etQueueSize.setFilters(new InputFilter[]{new InputFilterMinMax("1", String.valueOf(Integer.MAX_VALUE))});

        // Prevent keyboard to appear when opening this activity
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        return rootView;
    }

        @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");

        super.onDestroy();

        // Do not forget to unregister the broadcastReceiver when destroying this fragment.
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
    }

    /**
     * This will request DummyService to initialize its Executor.
     *
     */
    @OnClick(R.id.btnInitializeThreadPool)
    public void onInitializeThreadPoolClick() {
        Log.v(TAG, "onInitThreadPoolClick");

        String strNumberOfTasks = etNumberOfTasks.getText().toString();
        String strCoreThreadPoolSize = etCoreThreadPoolSize.getText().toString();
        String strMaximumThreadPoolSize = etMaximumThreadPoolSize.getText().toString();
        String strQueueSize = etQueueSize.getText().toString();

        if (strNumberOfTasks.isEmpty()) {
            Toast.makeText(getContext(), "Number of tasks cannot be empty", Toast.LENGTH_LONG).show();
            etNumberOfTasks.requestFocus();
            return;
        }

        if (strCoreThreadPoolSize.isEmpty()) {
            Toast.makeText(getContext(), "Core thread pool size cannot be empty", Toast.LENGTH_LONG).show();
            etCoreThreadPoolSize.requestFocus();
            return;
        }

        if (strMaximumThreadPoolSize.isEmpty()) {
            Toast.makeText(getContext(), "Maximum thread pool size cannot be empty", Toast.LENGTH_LONG).show();
            etMaximumThreadPoolSize.requestFocus();
            return;
        }

        if ((Integer.parseInt(strCoreThreadPoolSize) > Integer.parseInt(strMaximumThreadPoolSize))) {
            Toast.makeText(getContext(), "Core thread pool size cannot be greater than maximum thread pool size", Toast.LENGTH_LONG).show();
            etCoreThreadPoolSize.requestFocus();
        }

        if (strQueueSize.isEmpty()) {
            Toast.makeText(getContext(), "Queue size cannot be empty", Toast.LENGTH_LONG).show();
            etQueueSize.requestFocus();
            return;
        }

        boolean preStartCoreThreads = cbPreStartCoreThreads.isChecked();

        Intent i = new Intent(getContext(), DummyService.class);
        i.setAction(Constants.INIT);

        // Inform whether core thread pool will be pre started.
        i.putExtra(Constants.PRE_START_CORE_THREADS, preStartCoreThreads);

        int rejectionMode = spRejectionMode.getSelectedItemPosition();
        switch (rejectionMode) {
            case 0:
                i.putExtra(Constants.REJECTION_MODE, Constants.ABORT_POLICY);
                break;

            case 1:
                i.putExtra(Constants.REJECTION_MODE, Constants.CALLER_RUNS_POLICY);
                break;

            case 3:
                i.putExtra(Constants.REJECTION_MODE, Constants.CUSTOM_REJECTION_POLICY);
                break;
        }

        i.putExtra(Constants.NUMBER_OF_TASKS,  Integer.parseInt(strNumberOfTasks));
        i.putExtra(Constants.CORE_THREAD_POOL_SIZE,  Integer.parseInt(strCoreThreadPoolSize));
        i.putExtra(Constants.MAXIMUM_THREAD_POOL_SIZE, Integer.parseInt(strMaximumThreadPoolSize));
            i.putExtra(Constants.QUEUE_SIZE, Integer.parseInt(strQueueSize));

        Log.d(TAG, "Starting DummyService with action INIT...");
        getContext().startService(i);
    }
}
