package ness.discovery.client.internal;

import com.nesscomputing.logging.Log;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

import ness.discovery.client.DiscoveryClientConfig;
import ness.discovery.client.ServiceInformation;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.codehaus.jackson.map.ObjectMapper;

import com.google.common.collect.Maps;

/**
 * Maintains the local announcements on the central service discovery directory and ensures that they are present even
 * if the client disconnects / reconnects.
 */
class ServiceDiscoveryAnnouncer extends ServiceDiscoveryTask
{
    private static final Log LOG = Log.findLog();

    /** Set of all the announcements managed by this client. */
    private final Set<ServiceInformation> localAnnouncements = new CopyOnWriteArraySet<ServiceInformation>();

    /** Map of the local announcements to the last generation in which it was present. */
    private final Map<String, Long> localAnnouncementGenerations = Maps.newHashMap();

    // Generation counter for the announcements. By setting the start value to 1, this
    // ensures an immediate run through the announcer.
    private volatile long lastAnnouncementGeneration = 0L;
    private final AtomicLong announcementGeneration = new AtomicLong(1L);

    ServiceDiscoveryAnnouncer(final DiscoveryClientConfig discoveryConfig, final ObjectMapper objectMapper)
    {
        super(discoveryConfig, objectMapper);
    }

    void unannounceAll()
    {
        localAnnouncements.clear();
        announcementGeneration.incrementAndGet();
    }

    void announce(final ServiceInformation serviceInformation)
    {
        localAnnouncements.add(serviceInformation);
        announcementGeneration.incrementAndGet();
    }

    void unannounce(final ServiceInformation serviceInformation)
    {
        localAnnouncements.remove(serviceInformation);
        announcementGeneration.incrementAndGet();
    }

    @Override
    void determineGeneration(final AtomicLong generation, final long tick)
    {
        final long currentAnnouncementGeneration = announcementGeneration.get();

        // Trigger a run through the work loop if the last announcement was before
        // the current generation.
        if (lastAnnouncementGeneration < currentAnnouncementGeneration) {
            generation.incrementAndGet();
            lastAnnouncementGeneration = currentAnnouncementGeneration;
        }
    }

    @Override
    void visit(final List<String> childNodes, final ZooKeeper zookeeper, final long currentGeneration) throws InterruptedException, KeeperException
    {
        // Loop through everything that we *should* announce, add them with the current generation to the
        // generation map.
        for (final ServiceInformation si : localAnnouncements) {
            // This announcement is safe, so increment its generation.
            localAnnouncementGenerations.put(si.getAnnouncementName(), currentGeneration);

            // If announcement is not present, announce it.
            if (!childNodes.contains(si.getAnnouncementName())) {
                LOG.debug("Need to announce %s", si.getAnnouncementName());
                final String childPath = getNodePath(si.getAnnouncementName());

                try {
                    final byte [] serialized = objectMapper.writeValueAsBytes(si);
                    zookeeper.create(childPath, serialized, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                    LOG.debug("Created announcement for %s", si.getAnnouncementName());
                }
                catch (IOException ioe) {
                    LOG.warn(ioe, "While generating announcement:");
                }
            }
        }

        // Now loop through everything that is currently present in the generation map. Remove everything that
        // is not marked with the current generation.
        for (final Iterator<Map.Entry<String, Long>> it = localAnnouncementGenerations.entrySet().iterator(); it.hasNext(); ) {
            final Map.Entry<String, Long> entry = it.next();

            if (entry.getValue() == currentGeneration) {
                LOG.trace("Announcement %s survives in generation %d", entry.getKey(), entry.getValue());
                continue;
            }
            else {
                LOG.trace("Announcement %s no longer present in generation %d", entry.getKey(), entry.getValue());
                final String childPath = getNodePath(entry.getKey());

                try {
                    zookeeper.delete(childPath, -1);
                    LOG.debug("Removed announcement for %s", entry.getKey());
                    it.remove();
                }
                catch (KeeperException ke) {
                    // The node disappeared under us. That should not happen, but
                    // test for it anyway.
                    if (ke.code() == Code.NONODE) {
                        LOG.trace("Node was already removed, ignoring");
                        it.remove();
                    }
                    else {
                        throw ke;
                    }
                }
            }
        }
    }
}
