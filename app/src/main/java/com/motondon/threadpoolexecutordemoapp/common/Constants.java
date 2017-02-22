package com.motondon.threadpoolexecutordemoapp.common;

public class Constants {

    // Task actions
    public static final String INIT = "INIT";
    public static final String START = "START";
    public static final String MAXIMUM_THREAD_POOL_SIZE_CHANGED = "MAXIMUM_THREAD_POOL_SIZE_CHANGED";
    public static final String STOP = "STOP";
    public static final String STOP_NOW = "STOP_NOW";
    public static final String DESTROY = "DESTROY";


    // These are some parameters callers will use when send actions to this service.
    public static final String REJECTION_MODE = "REJECTION_MODE";
    public static final String NUMBER_OF_TASKS = "NUMBER_OF_TASKS";
    public static final String NUMBER_OF_REQUESTED_TASKS = "NUMBER_OF_REQUESTED_TASKS";
    public static final String NUMBER_OF_NOT_COMPLETED_TASKS = "NUMBER_OF_NOT_COMPLETED_TASKS";
    public static final String CORE_THREAD_POOL_SIZE = "CORE_THREAD_POOL_SIZE";
    public static final String MAXIMUM_THREAD_POOL_SIZE = "MAXIMUM_THREAD_POOL_SIZE";
    public static final String QUEUE_SIZE = "QUEUE_SIZE";
    public static final String PRE_START_CORE_THREADS = "PRE_START_CORE_THREADS";

    // Parameters used by the callers depends on the user choice
    public static final int ABORT_POLICY = 0;
    public static final int CALLER_RUNS_POLICY = 1;
    public static final int CUSTOM_REJECTION_POLICY = 3;

    // Task results
    public static final String TASK_ENQUEUED = "TASK_ENQUEUED";
    public static final String TASK_REJECTED = "TASK_REJECTED";
    public static final String TASK_STARTED = "TASK_STARTED";
    public static final String TASK_FINISHED = "TASK_FINISHED";

    // Thread Pool events
    public static final String QUEUE_SIZE_CHANGED = "QUEUE_SIZE_CHANGED";
    public static final String ACTIVE_THREADS_SIZE_CHANGED = "ACTIVE_THREADS_SIZE_CHANGED";
    public static final String CURRENT_ACTIVE_THREADS_SIZE = "CURRENT_ACTIVE_THREADS_SIZE";
    public static final String CURRENT_QUEUE_SIZE = "CURRENT_QUEUE_SIZE";
    public static final String NUMBER_OF_THREADS_IN_THE_POOL = "NUMBER_OF_THREADS_IN_THE_POOL";
    public static final String NUMBER_OF_THREADS = "NUMBER_OF_THREADS";

    // Thread Pool result
    public static final String THREAD_POOL_HAS_NOT_FINALIZED = "THREAD_POOL_HAS_NOT_FINALIZED";
    public static final String THREAD_POOL_FINALIZED_DUE_SHUTDOWN_NOW = "THREAD_POOL_FINALIZED_DUE_SHUTDOWN_NOW";
    public static final String THREAD_POOL_FINALIZED = "THREAD_POOL_FINALIZED";

    // These two constants are used by the DummyService after it starts an executor (in case of success or failure).
    public static final String THREAD_POOL_INITIALIZED = "THREAD_POOL_INITIALIZED";
    public static final String THREAD_POOL_INITIALIZATION_FAILED = "THREAD_POOL_INITIALIZATION_FAILED";
}
