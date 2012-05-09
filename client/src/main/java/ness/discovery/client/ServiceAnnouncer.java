package ness.discovery.client;

import com.nesscomputing.lifecycle.Lifecycle;
import com.nesscomputing.lifecycle.LifecycleListener;
import com.nesscomputing.lifecycle.LifecycleStage;
import com.nesscomputing.logging.Log;

import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * Announces Services to service discovery.
 */
public class ServiceAnnouncer
{
    private static final Log LOG = Log.findLog();

    private Set<ServiceInformation> services = null;

    private final DiscoveryClient discoveryClient;

    @Inject
    public ServiceAnnouncer(final DiscoveryClient discoveryClient)
    {
        this.discoveryClient = discoveryClient;
    }

    @Inject(optional=true)
    void injectLifecycle(final Lifecycle lifecycle)
    {
        lifecycle.addListener(LifecycleStage.ANNOUNCE_STAGE, new LifecycleListener() {
            @Override
            public void onStage(final LifecycleStage stage) {
                ServiceAnnouncer.this.announce();
            }
        });

        lifecycle.addListener(LifecycleStage.UNANNOUNCE_STAGE, new LifecycleListener() {
            @Override
            public void onStage(final LifecycleStage stage) {
                ServiceAnnouncer.this.unannounce();
            }
        });
    }

    @Inject(optional = true)
    protected void setServices(final Set<ServiceInformation> services)
    {
        this.services = services;
    }

    void announce()
    {
        LOG.debug("Service announcer is about to announce %s", services);
        Preconditions.checkState(discoveryClient != null, "No discovery client injected!");

        if (services != null) {
            for (ServiceInformation si : services) {
                discoveryClient.announce(si);
            }
        }
    }

    void unannounce()
    {
        LOG.debug("Service announcer is about to unannounce %s", services);
        Preconditions.checkState(discoveryClient != null, "No discovery client injected!");

        if (services != null) {
            for (ServiceInformation si : services) {
                discoveryClient.unannounce(si);
            }
        }
    }
}
