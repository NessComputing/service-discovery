package ness.discovery.job;

import static java.lang.String.format;
import com.nesscomputing.lifecycle.Lifecycle;
import com.nesscomputing.lifecycle.LifecycleListener;
import com.nesscomputing.lifecycle.LifecycleStage;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import ness.discovery.client.DiscoveryClientConfig;
import ness.discovery.client.DiscoveryClientModule;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Accept jobs to be run on top of zookeeper and executes them.
 */
public class ZookeeperJobProcessor
{
    private final BlockingQueue<JobWrapper> jobQueue = new ArrayBlockingQueue<JobWrapper>(20);

    private final Thread processingThread;

    @Inject
    public ZookeeperJobProcessor(@Named(DiscoveryClientModule.ZOOKEEPER_CONNECT_NAME) final String connectString,
                                 final DiscoveryClientConfig discoveryClientConfig)
    {
        this.processingThread = new Thread(new JobProcessingRunnable(connectString, discoveryClientConfig.getTickInterval().getMillis()));
        this.processingThread.setName("zookeeper-job-processor");
        this.processingThread.setDaemon(true);
    }

    @Inject(optional=true)
    void injectLifecycle(final Lifecycle lifecycle)
    {
        lifecycle.addListener(LifecycleStage.START_STAGE, new LifecycleListener() {
            @Override
            public void onStage(final LifecycleStage stage) {
                ZookeeperJobProcessor.this.start();
            }
        });
        lifecycle.addListener(LifecycleStage.STOP_STAGE, new LifecycleListener() {
            @Override
            public void onStage(final LifecycleStage stage) {
                ZookeeperJobProcessor.this.stop();
            }
        });
    }

    public void start()
    {
        processingThread.start();
    }

    public void stop()
    {
        processingThread.interrupt();
        try {
            processingThread.join();
        }
        catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    public Future<ZookeeperJob> submitJob(final ZookeeperJob job, final int retries, final int timeout, final TimeUnit timeoutUnit)
        throws InterruptedException
    {
        final JobWrapper jobWrapper = new JobWrapper(job, retries);
        if (jobQueue.offer(jobWrapper, timeout, timeoutUnit)) {
            return jobWrapper;
        }
        else {
            return null;
        }
    }

    public Future<ZookeeperJob> submitJob(final ZookeeperJob job, final int retries)
        throws InterruptedException
    {
        final JobWrapper jobWrapper = new JobWrapper(job, retries);
        if (jobQueue.offer(jobWrapper)) {
            return jobWrapper;
        }
        else {
            return null;
        }
    }


    public class JobProcessingRunnable extends ZookeeperProcessingTask
    {
        private int retries = 0;
        private JobWrapper currentJob = null;

        JobProcessingRunnable(final String connectString,
                              final long tickInterval)
        {
            super(connectString, tickInterval);
        }

        @Override
        public long determineCurrentGeneration(final AtomicLong generation, final long tick)
        {
            if (!jobQueue.isEmpty()) {
                return generation.incrementAndGet();
            }
            else {
                return generation.get();
            }
        }

        @Override
        protected boolean doWork(final ZooKeeper zookeeper, final long tick) throws KeeperException, IOException
        {
            boolean result = true;

            if (currentJob == null) {
                currentJob = jobQueue.poll();
                retries = currentJob.getRetries();
            }
            if (currentJob != null) {
                if (currentJob.isCancelRequested()) {
                    currentJob.complete(JobWrapper.State.CANCEL_STATE);
                    currentJob = null;
                }
                else {
                    try {
                        if (currentJob.execute(zookeeper)) {
                            currentJob.complete(JobWrapper.State.DONE_STATE);
                            currentJob = null;
                        }
                        else {
                            result = processRetry();
                        }
                    }
                    catch (Throwable t) {
                        processRetry();
                        Throwables.propagateIfInstanceOf(t, KeeperException.class);
                        Throwables.propagateIfInstanceOf(t, IOException.class);
                        throw Throwables.propagate(t);
                    }
                }
            }
            return result;
        }

        private boolean  processRetry()
        {
            if (--retries > 0) {
                // Run was not successful, run retries.
                return false;
            }
            else {
                currentJob.complete(JobWrapper.State.FAILED_STATE);
                currentJob = null;
                // Run was successful, job failed.
                return true;
            }
        }
    }

    public static class JobWrapper implements Future<ZookeeperJob>
    {
        static enum State {
            RUNNING_STATE, CANCEL_STATE, DONE_STATE, FAILED_STATE;
        }

        private final ZookeeperJob job;
        private final int retries;
        private final CountDownLatch latch = new CountDownLatch(1);

        private volatile State state = State.RUNNING_STATE;
        private volatile boolean cancelRequested = false;

        JobWrapper(final ZookeeperJob job, final int retries)
        {
            this.job = job;
            this.retries = retries;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning)
        {
            if (isCancelled()) {
                return false;
            }
            else {
                cancelRequested = true;
                return true;
            }
        }

        @Override
        public boolean isCancelled()
        {
            return state == State.CANCEL_STATE;
        }

        @Override
        public boolean isDone()
        {
            return state == State.DONE_STATE;
        }

        @Override
        public ZookeeperJob get() throws InterruptedException, ExecutionException
        {
            latch.await();
            return doGet();
        }

        @Override
        public ZookeeperJob get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
        {
            if (!latch.await(timeout, unit)) {
                throw new TimeoutException();
            }
            return doGet();
        }

        private ZookeeperJob doGet() throws ExecutionException
        {
            switch (state) {
                case FAILED_STATE:
                    throw new ExecutionException("Job could not be completed!", null);
                case DONE_STATE:
                case CANCEL_STATE:
                    return job;
                default:
                    throw new ExecutionException(format("JobWrapper is in an illegal state (%s)!", state), null);
            }
        }

        boolean execute(final ZooKeeper zookeeper) throws IOException, KeeperException
        {
            return job.execute(zookeeper);
        }

        int getRetries()
        {
            return retries;
        }

        boolean isCancelRequested()
        {
            return cancelRequested;
        }

        void complete(final State state)
        {
            this.state = state;
            latch.countDown();
        }
    }
}
