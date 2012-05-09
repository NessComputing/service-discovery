package ness.discovery.server;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ness.discovery.client.DiscoveryClientModule;
import ness.discovery.job.ZookeeperJob;
import ness.discovery.job.ZookeeperJobProcessor;
import ness.discovery.server.job.BuildPathJob;
import ness.discovery.server.resources.ServiceLookupResource;
import ness.discovery.server.resources.StateOfTheWorldResource;
import ness.discovery.server.zookeeper.ZookeeperCleanupJob;
import ness.discovery.server.zookeeper.ZookeeperModule;

import org.joda.time.Duration;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.nesscomputing.config.Config;
import com.nesscomputing.config.ConfigProvider;
import com.nesscomputing.galaxy.GalaxyConfigModule;
import com.nesscomputing.httpserver.HttpServerModule;
import com.nesscomputing.httpserver.standalone.StandaloneServer;
import com.nesscomputing.jackson.NessJacksonModule;
import com.nesscomputing.jersey.NessJerseyServletModule;
import com.nesscomputing.jmx.jolokia.JolokiaModule;
import com.nesscomputing.jmx.starter.guice.JmxStarterModule;
import com.nesscomputing.logging.Log;
import com.nesscomputing.quartz.NessQuartzModule;
import com.nesscomputing.quartz.QuartzJobBinder;

/**
 * Discovery Server main class.
 */
public class DiscoveryServerMain extends StandaloneServer
{
    private static final Log LOG = Log.findLog();

    public static void main(final String [] args)
    {
        final StandaloneServer server = new DiscoveryServerMain();
        server.startServer();
    }

    @Inject
    private ZookeeperJobProcessor jobProcessor;

    @Inject
    private DiscoveryServerConfig discoveryConfig;

    @Override
    public Module getMainModule(final Config config)
    {
        return new AbstractModule() {
            @Override
            public void configure()
            {
                install(new GalaxyConfigModule());
                install(new JmxStarterModule(config));
                install(new HttpServerModule(config));
                install(new JolokiaModule());
                install(new ZookeeperModule(config));
                // Install a read only module for the client.
                install(new DiscoveryClientModule(true));
                install(new NessJacksonModule());
                install(new NessJerseyServletModule(config));
                install(new NessQuartzModule(config));

                bind(ZookeeperCleanupJob.class);
                QuartzJobBinder.bindQuartzJob(binder(), ZookeeperCleanupJob.class)
                    .conditional("zookeeper-cleanup").delay(Duration.standardMinutes(1)).repeat(Duration.standardHours(8)).register();

                bind(DiscoveryServerConfig.class).toProvider(ConfigProvider.of(DiscoveryServerConfig.class)).in(Scopes.SINGLETON);
                bind(ZookeeperJobProcessor.class).in(Scopes.SINGLETON);

                bind(StateOfTheWorldResource.class);
                bind(ServiceLookupResource.class);
            }

        };
    }

    @Override
    public void startServer()
    {
        super.startServer();

        try {
            LOG.info("Creating discovery root nodes (%s)", discoveryConfig.getRoot());
            final Future<ZookeeperJob> jobFuture = jobProcessor.submitJob(new BuildPathJob(discoveryConfig.getRoot()), 5, 1, TimeUnit.MINUTES);
            if (jobFuture == null) {
                LOG.error("Could not submit job to create root nodes for discovery!");
            }
            try {
                jobFuture.get(1, TimeUnit.MINUTES);
            }
            catch (TimeoutException te) {
                LOG.error(te, "Root node creation did not finish in time!");
            }
            catch (ExecutionException ee) {
                LOG.error(ee, "Root node creation failed!");
            }
        }
        catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
