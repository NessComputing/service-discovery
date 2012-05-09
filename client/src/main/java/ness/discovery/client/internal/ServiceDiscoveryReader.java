package ness.discovery.client.internal;

import com.nesscomputing.logging.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import ness.discovery.client.DiscoveryClientConfig;
import ness.discovery.client.ServiceInformation;

import org.apache.zookeeper.AsyncCallback.DataCallback;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.codehaus.jackson.map.ObjectMapper;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Reads the current list of service announcements from Zookeeper and updates the
 * state of the world accordingly.
 */
public class ServiceDiscoveryReader extends ServiceDiscoveryTask
{
    private static final Log LOG = Log.findLog();

    /** Internal list of bad announcement nodes that could not be read for whatever reason. */
    private final Map<String, Long> badNodes = new ConcurrentHashMap<String, Long>();

    private final StateOfTheWorldHolder stateHolder;

    private final long penaltyTime;

    ServiceDiscoveryReader(final DiscoveryClientConfig discoveryConfig,
                           final ObjectMapper objectMapper,
                           final StateOfTheWorldHolder stateHolder)

    {
        super(discoveryConfig, objectMapper);
        this.stateHolder = stateHolder;

        this.penaltyTime = discoveryConfig.getPenaltyTime().getMillis() * 1000000L;
    }

    @Override
    void visit(final List<String> childNodes, final ZooKeeper zookeeper, final long tick) throws InterruptedException
    {
        final Map<String, List<ServiceInformation>> serviceMap = new HashMap<String, List<ServiceInformation>>();

        if (childNodes.size() > 0) {
            final List<ServiceInformation> rawServices = new ArrayList<ServiceInformation>(childNodes.size());
            final CountDownLatch latch = new CountDownLatch(childNodes.size());

            final long now = System.nanoTime();

            for (final String child : childNodes) {

                final String childPath = getNodePath(child);

                if (badNodes.containsKey(childPath)) {
                    final Long penaltyEndsTime = badNodes.get(childPath);
                    if (penaltyEndsTime != null && penaltyEndsTime > now) {
                        // Decrement the countdown latch, because there will be no callback for this
                        // node.
                        latch.countDown();
                        // Ignore a bad node for a while.
                        continue;
                    }
                    LOG.info("Unmarking %s as a bad node!", childPath);
                    badNodes.remove(childPath);
                }

                zookeeper.getData(childPath, false, new DataCallback() {
                    @Override
                    public void processResult(final int rc, final String path, final Object ctx, final byte[] data, final Stat stat) {

                        ServiceInformation si = null;
                        try {
                            if (data != null && data.length > 0) {
                                si = objectMapper.readValue(data, ServiceInformation.class);
                                LOG.trace("%s contains %s", path, si);
                            }
                            else {
                                // This can sometimes happen if a node that we want to inspect
                                // disappears between callback post and callback processing.
                                LOG.trace("Got callback but no data!");
                            }

                        }
                        catch (IOException ioe) {
                            LOG.debug(ioe, "While deserializing %s", new String(data, Charsets.UTF_8));
                            LOG.info("Marking %s as a bad node!", path);
                            // Put a bad node into the penalty box.
                            badNodes.put(path, now + penaltyTime);
                        }
                        finally {
                            synchronized (rawServices) {
                                if (si != null) {
                                    rawServices.add(si);
                                }
                            }
                            latch.countDown();
                        }
                    }
                }, null);
            }

            if (!latch.await(discoveryConfig.getZookeeperTimeout().getMillis(), TimeUnit.MILLISECONDS)) {
                LOG.warn("Timeout waiting for callbacks, some nodes were not parsed.");
            }

            // Make sure that even with late callbacks, this will not throw spurious ConcurrentModificationExceptions
            synchronized (rawServices) {
                for (final ServiceInformation si : rawServices) {
                    List<ServiceInformation> services = serviceMap.get(si.getServiceName());
                    if (services == null) {
                        services = new ArrayList<ServiceInformation>();
                        serviceMap.put(si.getServiceName(), services);
                    }
                    services.add(si);
                }
            }
        }
        
        Map<String, ConsistentRingGroup> serviceGroups = Maps.newHashMap();
        for (Map.Entry<String, List<ServiceInformation>> entry: serviceMap.entrySet()) {
        	ConsistentRingGroup currentGroup = stateHolder.getState().get(entry.getKey());
        	//Rebuilding a group is kind of expensive, so reuse the old group if it hasn't changed
			if (currentGroup != null && Sets.newHashSet(entry.getValue()).equals(Sets.newHashSet(currentGroup.getAll()))) {
        		serviceGroups.put(entry.getKey(), currentGroup);
        	} else {
        		serviceGroups.put(entry.getKey(), new ConsistentRingGroup(entry.getValue()));
        	}
        }
        stateHolder.setState(serviceGroups);
    }
}
