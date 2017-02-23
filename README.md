# ThreadPoolExecutorDemoApp

This project demonstrates some topics related to the ThreadPoolExecutor that is part of the Executor framework.

You can easily adjust some of the executor configurations like:

  - Rejection policy (including a custom rejection policy)
  - The number of tasks to be executed
  - Core thread pool size
  - Maximum thread pool size
  - Queue size
  - Configure core threads to be pre-started
  - Safe stop a test
  - Explicitly stop a test

During a test execution you will be able to:

  - See the queue size as it changes
  - See the number of active threads
  - See the number of thread in the pool
  - Dynamically change the number of maximum thread pool

In the end it will give a summary of what have done:

  - Number of Enqueued Tasks
  - Number of Started Tasks
  - Number of Completed Tasks
  - Number of Not Completed Tasks
  - Number of Rejected Tasks

The idea is to help developers to better visualize some of the innumerous possibilities of configuration this framework offers.

Please, refer to [this article](http://androidahead.com/2017/02/23/threadpoolexecutor-on-android-a-practical-example/) for detailed information.

![Demo](https://cloud.githubusercontent.com/assets/4574670/23194707/7e191084-f88e-11e6-8dd3-30b983e64fa5.gif)

# License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details



