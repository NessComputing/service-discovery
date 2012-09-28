/**
 * Copyright (C) 2012 Ness Computing, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nesscomputing.service.discovery.server;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Scopes;

import org.joda.time.Duration;

import com.nesscomputing.config.Config;
import com.nesscomputing.config.ConfigProvider;
import com.nesscomputing.jmx.jolokia.JolokiaModule;
import com.nesscomputing.lifecycle.LifecycleStage;
import com.nesscomputing.lifecycle.ServiceDiscoveryLifecycle;
import com.nesscomputing.lifecycle.guice.LifecycleModule;
import com.nesscomputing.logging.Log;
import com.nesscomputing.quartz.NessQuartzModule;
import com.nesscomputing.quartz.QuartzJobBinder;
import com.nesscomputing.server.StandaloneServer;
import com.nesscomputing.server.templates.BasicGalaxyServerModule;
import com.nesscomputing.service.discovery.client.DiscoveryClientModule;
import com.nesscomputing.service.discovery.job.ZookeeperJob;
import com.nesscomputing.service.discovery.job.ZookeeperJobProcessor;
import com.nesscomputing.service.discovery.server.announce.ConfigStaticAnnouncer;
import com.nesscomputing.service.discovery.server.job.BuildPathJob;
import com.nesscomputing.service.discovery.server.resources.ServiceLookupResource;
import com.nesscomputing.service.discovery.server.resources.StateOfTheWorldResource;
import com.nesscomputing.service.discovery.server.resources.StaticAnnouncementResource;
import com.nesscomputing.service.discovery.server.zookeeper.ZookeeperCleanupJob;
import com.nesscomputing.service.discovery.server.zookeeper.ZookeeperModule;

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
                install(new BasicGalaxyServerModule(config));

                install(new JolokiaModule());
                install(new ZookeeperModule(config));
                install(new DiscoveryClientModule(false));
                install(new NessQuartzModule(config));

                bind(ZookeeperCleanupJob.class);
                QuartzJobBinder.bindQuartzJob(binder(), ZookeeperCleanupJob.class)
                               .conditional("zookeeper-cleanup")
                               .delay(Duration.standardMinutes(1))
                               .repeat(Duration.standardHours(8))
                               .register();

                bind(DiscoveryServerConfig.class).toProvider(ConfigProvider.of(DiscoveryServerConfig.class)).in(Scopes.SINGLETON);
                bind(ZookeeperJobProcessor.class).in(Scopes.SINGLETON);

                bind(StateOfTheWorldResource.class);
                bind(ServiceLookupResource.class);
                bind(StaticAnnouncementResource.class);

                bind(ConfigStaticAnnouncer.class).asEagerSingleton();
            }
        };
    }

    @Override
    protected Module getLifecycleModule()
    {
        return new LifecycleModule(ServiceDiscoveryLifecycle.class);
    }

    @Override
    protected LifecycleStage getStartStage()
    {
        return LifecycleStage.ANNOUNCE_STAGE;
    }

    @Override
    public String getServerType()
    {
        return "discovery";
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
            catch (final TimeoutException te) {
                LOG.error(te, "Root node creation did not finish in time!");
            }
            catch (final ExecutionException ee) {
                LOG.error(ee, "Root node creation failed!");
            }
        }
        catch (final InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
