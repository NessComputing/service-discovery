package ness.discovery.client.internal;

import com.nesscomputing.lifecycle.Lifecycle;
import com.nesscomputing.lifecycle.LifecycleListener;
import com.nesscomputing.lifecycle.LifecycleStage;
import com.nesscomputing.logging.Log;

import ness.discovery.client.DiscoveryClient;
import ness.discovery.client.DiscoveryClientConfig;
import ness.discovery.client.DiscoveryClientModule;
import ness.discovery.client.ServiceInformation;

import org.codehaus.jackson.map.ObjectMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * Read/Write service discovery client. Supports service lookup and announcing of local services using Zookeeper.
 */
@Singleton
public class DiscoveryClientImpl extends ReadOnlyDiscoveryClientImpl implements DiscoveryClient
 {
     private static final Log LOG = Log.findLog();

     private final ServiceDiscoveryAnnouncer announcer;

     @Inject
     public DiscoveryClientImpl(@Named(DiscoveryClientModule.ZOOKEEPER_CONNECT_NAME) final String connectString,
                                final DiscoveryClientConfig discoveryConfig,
                                final ObjectMapper objectMapper)
     {
         super(connectString, discoveryConfig, objectMapper);

         announcer = new ServiceDiscoveryAnnouncer(discoveryConfig, objectMapper);

         if (discoveryConfig.isAnnounceEnabled()) {
             serviceDiscoveryVisitors.add(announcer);
         }
         else {
             LOG.info("Service announcement is administratively disabled!");
         }
     }

     @Inject(optional=true)
     @Override
     public void injectLifecycle(final Lifecycle lifecycle)
     {
         super.injectLifecycle(lifecycle);

         lifecycle.addListener(LifecycleStage.UNANNOUNCE_STAGE, new LifecycleListener() {
             @Override
             public void onStage(final LifecycleStage stage) {
                 DiscoveryClientImpl.this.unannounceAll();
             }
         });
     }

     @Override
     public void unannounceAll()
     {
         announcer.unannounceAll();
     }

     @Override
     public void announce(final ServiceInformation serviceInformation)
     {
         LOG.debug("Announcing %s", serviceInformation);
         announcer.announce(serviceInformation);
     }

     @Override
     public void unannounce(final ServiceInformation serviceInformation)
     {
         LOG.debug("Unannouncing %s", serviceInformation);
         announcer.unannounce(serviceInformation);
     }
}
