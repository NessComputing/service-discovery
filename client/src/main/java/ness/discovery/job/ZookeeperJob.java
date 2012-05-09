package ness.discovery.job;

import java.io.IOException;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

/**
 * An unit of work that needs to be executed with a valid zookeeper connection.
 */
public abstract class ZookeeperJob
{
    public ZookeeperJob()
    {
    }

    /**
     * Execute the unit of work. The zookeeper is connected when this method is called but can
     * be disconnected at any time.
     *
     * @return True if the work was successful, false if not, then it might be retried.
     */
    protected abstract boolean execute(final ZooKeeper zookeeper)
        throws KeeperException, IOException;
}
