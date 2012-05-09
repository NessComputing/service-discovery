package ness.discovery.job;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import com.google.common.base.Preconditions;
import com.nesscomputing.logging.Log;

/**
 * Driver task to maintain a zookeeper connection and execute work on it. Handles connection
 * loss and reconnection of the client.
 */
@edu.umd.cs.findbugs.annotations.SuppressWarnings("SF_SWITCH_NO_DEFAULT")
public abstract class ZookeeperProcessingTask implements Watcher, Runnable
{
    private static final Log LOG = Log.findLog();

    private final Lock zookeeperLock = new ReentrantLock();

    private volatile boolean connected = false;
    private volatile ZooKeeper zookeeper = null;

    // Current generation. Processing only happens if the generation
    // changes (which implies a local state change). Start with 1 so that a
    // full scan happens right after connection.
    private final AtomicLong generation = new AtomicLong(1L);
    private final AtomicLong ticker = new AtomicLong(0L);

    private final String connectString;
    private final long tickInterval;

    public ZookeeperProcessingTask(final String connectString, final long tickInterval)
    {
        Preconditions.checkArgument(!StringUtils.isBlank(connectString), "empty connect string");

        this.connectString = connectString;
        this.tickInterval = tickInterval;
    }

    @Override
    public void run()
    {
        long lastGeneration = 0L;

        try {
            while(true) {
                try {
                    final long tick = ticker.getAndIncrement();
                    if (!connected) {
                        openZookeeper();
                    }
                    if (connected) {
                        final long currentGeneration = determineCurrentGeneration(generation, tick);
                        if (lastGeneration < currentGeneration) {
                            LOG.debug("Processing...");

                            try {
                                zookeeperLock.lock();
                                // This can happen if the zookeeper object gets
                                // removed by closeZookeeper after the connection check
                                // and before the lock protects it for doWork.
                                if (zookeeper != null && doWork(zookeeper, tick)) {
                                    // Record this as the last successful run state.
                                    // If doWork threw a KeeperException, it will
                                    // be re-run.
                                    lastGeneration = currentGeneration;
                                }
                            }
                            finally {
                                zookeeperLock.unlock();
                            }
                        }
                    }
                }
                catch (IOException ioe) {
                    LOG.warn(ioe, "While processing work: ");
                }
                catch (KeeperException ke) {
                    processKeeperException(ke);
                }

                Thread.sleep(tickInterval);
                LOG.trace("Tick...");
            }
        }
        catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            LOG.debug("Interrupted, exiting...");
        }
        finally {
            closeZookeeper();
        }
    }

    protected long determineCurrentGeneration(final AtomicLong generation, final long tick)
    {
        return generation.get();
    }

    protected abstract boolean doWork(final ZooKeeper zookeeper, final long tick) throws IOException, KeeperException, InterruptedException;

    @Override
    public void process(WatchedEvent event)
    {
        LOG.debug("Received '%s' event", event.getState());
        switch(event.getState()) {
        case AuthFailed:
            LOG.warn("Got an auth request from zookeeper. Server config is not compatible to this client!");
            break;
        case SyncConnected:
            LOG.trace("Session connected");
            connected = true;

            switch(event.getType()) {
            case None:
                break;
            default:
                LOG.trace("Zookeeper state changed: %s", event.getType());
                // Forces running of the work loop.
                generation.incrementAndGet();
                break;
            }

            break;
        case Disconnected:
            LOG.trace("Session disconnected, waiting for reconnect");
            connected = false;
            break;
        case Expired:
            LOG.trace("Session expired, closing zookeeper.");
            connected = false;
            closeZookeeper();
        default:
            // Huh?
            LOG.debug("Failed to process unknown state %s", event.getState());
            break;
        }
    }

    private void processKeeperException(final KeeperException ke)
        throws InterruptedException
    {
        switch(ke.code()) {
        case CONNECTIONLOSS:
            LOG.trace("Connection lost, waiting for reconnect");
            connected = false;
            break;

        case SESSIONEXPIRED:
            LOG.trace("Session expired, closing zookeeper.");
            connected = false;
            closeZookeeper();
            break;

        default:
            LOG.warn("Zookeeper Problem: %s", ke.code());
            break;
        }
    }

    private void openZookeeper()
    {
        try {
            zookeeperLock.lock();
            if (this.zookeeper == null) {
                this.zookeeper = new ZooKeeper(connectString, 3000, this);
            }
        } catch (IOException ioe) {
            LOG.warn(ioe, "Could not connect to zookeeper, retrying...");
        }
        finally {
            zookeeperLock.unlock();
        }
    }

    private void closeZookeeper()
    {
        try {
            zookeeperLock.lock();
            if (this.zookeeper != null) {
                this.zookeeper.close();
                this.zookeeper = null;
                this.connected = false;
            }
        }
        catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        finally {
            zookeeperLock.unlock();
        }
    }
}
