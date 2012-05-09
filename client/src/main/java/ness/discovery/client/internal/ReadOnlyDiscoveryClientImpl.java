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
package ness.discovery.client.internal;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import ness.discovery.client.DiscoveryClientConfig;
import ness.discovery.client.DiscoveryClientModule;
import ness.discovery.client.ReadOnlyDiscoveryClient;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.nesscomputing.lifecycle.Lifecycle;
import com.nesscomputing.lifecycle.LifecycleListener;
import com.nesscomputing.lifecycle.LifecycleStage;
import com.nesscomputing.logging.Log;

/**
 * Read-only service discovery client. Supports service lookup using the zookeeper implementation.
 */
@Singleton
public class ReadOnlyDiscoveryClientImpl extends AbstractDiscoveryClient implements ReadOnlyDiscoveryClient
{
    private static final Log LOG = Log.findLog();

    private final String connectString;
    private final DiscoveryClientConfig discoveryConfig;

    protected final Set<ServiceDiscoveryTask> serviceDiscoveryVisitors = Sets.newHashSet();

    private volatile Thread discoveryThread = null;

    @Inject
    public ReadOnlyDiscoveryClientImpl(@Named(DiscoveryClientModule.ZOOKEEPER_CONNECT_NAME) final String connectString,
                                       final DiscoveryClientConfig discoveryConfig,
                                       final ObjectMapper objectMapper)
    {
        super(discoveryConfig.isEnabled());

        if (discoveryConfig.isEnabled()) {
            Preconditions.checkState(!StringUtils.isBlank(connectString), "no zookeeper server configured!");
        }

        this.connectString = connectString;
        this.discoveryConfig = discoveryConfig;

        serviceDiscoveryVisitors.add(new ServiceDiscoveryReader(discoveryConfig, objectMapper, getStateOfTheWorldHolder()));
    }

    @Inject(optional=true)
    public void injectLifecycle(final Lifecycle lifecycle)
    {
        lifecycle.addListener(LifecycleStage.START_STAGE, new LifecycleListener() {
            @Override
            public void onStage(final LifecycleStage stage) {
                ReadOnlyDiscoveryClientImpl.this.start();
            }
        });

        lifecycle.addListener(LifecycleStage.STOP_STAGE, new LifecycleListener() {
            @Override
            public void onStage(final LifecycleStage stage) {
                ReadOnlyDiscoveryClientImpl.this.stop();
            }
        });
    }

    public synchronized void start()
    {
        if (!discoveryConfig.isEnabled()) {
            LOG.info("Discovery service is disabled.");
        }
        else {
            if (discoveryThread == null) {
                discoveryThread = new Thread(new ServiceDiscoveryRunnable(connectString, discoveryConfig, serviceDiscoveryVisitors));
                discoveryThread.setName("service-discovery-thread");
                discoveryThread.setDaemon(true);
                discoveryThread.start();

                LOG.info("Started Discovery client");
            }
            else {
                LOG.warn("Double start attempted!");
            }

            LOG.debug("Waiting for the world to change!");
            try {
                // The magic one second wait for service discovery to catch up.
                if (waitForWorldChange(1, TimeUnit.SECONDS)) {
                    LOG.debug("World just changed!");
                }
                else {
                    LOG.info("Did not receive a world changing event before timeout, just continuing on...");
                }
            }
            catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public synchronized void stop()
    {
        if (discoveryConfig.isEnabled()) {
            if (discoveryThread == null) {
                LOG.warn("Client was never started!");
            }
            else {
                discoveryThread.interrupt();
                try {
                    discoveryThread.join(discoveryConfig.getZookeeperTimeout().getMillis());
                }
                catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                LOG.info("Stopped Discovery client");
            }
        }
    }
}
