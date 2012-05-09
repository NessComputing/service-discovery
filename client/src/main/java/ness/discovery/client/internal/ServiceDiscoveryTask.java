package ness.discovery.client.internal;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import ness.discovery.client.DiscoveryClientConfig;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Base class for a task run by the main service discovery thread.
 */
abstract class ServiceDiscoveryTask
{
    protected final DiscoveryClientConfig discoveryConfig;
    protected final ObjectMapper objectMapper;

    protected ServiceDiscoveryTask (final DiscoveryClientConfig discoveryConfig,
                                    final ObjectMapper objectMapper)
    {
        this.discoveryConfig = discoveryConfig;
        this.objectMapper = objectMapper;
    }

    protected final String getNodePath(final String nodeName)
    {
        return discoveryConfig.getRoot() + "/" + nodeName;
    }

    /**
     * Visit a list of nodes.
     */
    abstract void visit(final List<String> childNodes, final ZooKeeper zookeeper, final long tick) throws KeeperException, InterruptedException;

    /**
     * Trigger a scan of the nodes on zookeeper by incrementing the generation counter.
     */
    void determineGeneration(final AtomicLong generation, final long tick)
    {
        // Do nothing, derived classes override this.
    }
}
