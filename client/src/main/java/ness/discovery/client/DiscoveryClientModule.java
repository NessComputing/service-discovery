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
package ness.discovery.client;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map;

import ness.discovery.client.internal.DiscoveryClientImpl;
import ness.discovery.client.internal.ReadOnlyDiscoveryClientImpl;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.nesscomputing.config.Config;
import com.nesscomputing.config.ConfigProvider;
import com.nesscomputing.httpclient.guice.HttpClientModule;
import com.nesscomputing.logging.Log;

/**
 * Module for the client side of service discovery.
 */
public class DiscoveryClientModule extends AbstractModule
{
    public static final String ZOOKEEPER_CONNECT_NAME = "_zookeeper_connect";
    public static final Named ZOOKEEPER_CONNECT_NAMED = Names.named(ZOOKEEPER_CONNECT_NAME);

    private static final Log LOG = Log.findLog();

    private final boolean readOnly;

    /**
     * Installs a read/write discovery client.
     */
    public DiscoveryClientModule()
    {
        this(false);
    }

    /**
     * Installs a read/write discovery client.
     *
     * @param readonly - True if the client should be read only (allow service lookup but no announcements.
     */
    public DiscoveryClientModule(final boolean readOnly)
    {
        this.readOnly = readOnly;
    }

    @Override
    public void configure()
    {
        bind(DiscoveryClientConfig.class).toProvider(ConfigProvider.of(DiscoveryClientConfig.class)).in(Scopes.SINGLETON);
        bind(ServiceURIConverter.class).in(Scopes.SINGLETON);

        if (readOnly) {
            bind(ReadOnlyDiscoveryClient.class).to(ReadOnlyDiscoveryClientImpl.class).in(Scopes.SINGLETON);
        }
        else {
            bind(DiscoveryClientImpl.class).in(Scopes.SINGLETON);
            bind(ReadOnlyDiscoveryClient.class).to(DiscoveryClientImpl.class).in(Scopes.SINGLETON);
            bind(DiscoveryClient.class).to(DiscoveryClientImpl.class).in(Scopes.SINGLETON);
            bind(ServiceAnnouncer.class).asEagerSingleton();
        }


        HttpClientModule.bindNewObserver(binder()).to(DiscoveryServiceInterceptor.class);
    }

    /**
     * Register a new Announcement with service discovery. This will only work with the read/write Discovery client and
     * it requires binding the ServiceAnnouncer as an eager singleton.
     * @return the binding builder you should register with
     */
    public static LinkedBindingBuilder<ServiceInformation> bindNewAnnouncement(final Binder binder)
    {
        return Multibinder.newSetBinder(binder, ServiceInformation.class).addBinding();
    }


    @Provides
    @Singleton
    @Named(ZOOKEEPER_CONNECT_NAME)
    String getConnectString(@Named(ZOOKEEPER_CONNECT_NAME) final Map<Integer, InetSocketAddress> zookeeperServers,
                            final DiscoveryClientConfig clientConfig)
    {
        final StringBuilder sb = new StringBuilder();

        for (final InetSocketAddress address : zookeeperServers.values()) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(address.getAddress().getHostAddress()).append(":").append(address.getPort());
        }

        if (sb.length() == 0) {
            if (clientConfig.isEnabled()) {
                throw new IllegalStateException("Service discovery is enabled but no servers found!");
            }
            else {
                LOG.info("No servers found!");
            }
        }
        return sb.toString();
    }

    @Provides
    @Singleton
    @Named(ZOOKEEPER_CONNECT_NAME)
    Map<Integer, InetSocketAddress> getZookeeperServers(final Config config, final DiscoveryClientConfig clientConfig)
    {
        Map<Integer, InetSocketAddress> results = Maps.newHashMap();

        if (!clientConfig.isEnabled()) {
            LOG.warn("Service Discovery is administratively disabled.");
        }
        else {
            final Configuration zookeeperConfig = config.getConfiguration("ness.zookeeper");

            // This can be explicitly given to support the "Three servers, one host" configuration.
            final String [] servers = zookeeperConfig.getStringArray("clientConnect");

            if (ArrayUtils.isNotEmpty(servers)) {
                LOG.debug("Found explicit 'ness.zookeeper.clientConnect' string (%s)", StringUtils.join(servers, ","));
                int serverId = 1;
                for (String server: servers) {
                    final String parts[] = StringUtils.split(server, ":");
                    InetSocketAddress serverAddress = new InetSocketAddress(parts[0], Integer.parseInt(parts[1]));
                    results.put(serverId++, serverAddress);
                }
            }
            else {
                LOG.debug("Building connectString from server configuration.");

                final int clientPort = zookeeperConfig.getInt("clientPort");
                LOG.debug("ness.zookeeper.clientPort is %d", clientPort);

                for (final Iterator<?> it = zookeeperConfig.getKeys("server"); it.hasNext(); ) {
                    final String key = it.next().toString();
                    final String [] keyElements = StringUtils.split(key, ".");
                    final String value = zookeeperConfig.getString(key);
                    final Integer serverId = Integer.parseInt(keyElements[keyElements.length-1]);
                    final String parts[] = StringUtils.split(value, ":");

                    InetSocketAddress serverAddress = new InetSocketAddress(parts[0], clientPort);
                    results.put(serverId, serverAddress);
                    LOG.debug("Server # %d : %s", serverId, serverAddress);
                }

                // If there are less than two servers, this is running in standalone mode. In that case,
                // use the clientAddress, because that is what the server will be using.
                if (results.size() < 2) {
                    LOG.info("Found less than two servers, falling back to clientPortAddress/clientPort!");
                    final String clientAddress = zookeeperConfig.getString("clientPortAddress");
                    Preconditions.checkState(clientAddress != null, "Client address must not be null!");

                    final InetSocketAddress serverAddress = new InetSocketAddress(clientAddress, clientPort);
                    LOG.debug("Server: %s", serverAddress);
                    results = ImmutableMap.of(1, serverAddress);
                }
            }
            if (LOG.isDebugEnabled()) {
                for (Map.Entry<Integer, InetSocketAddress> entry: results.entrySet()) {
                    LOG.debug("Server # %d : %s", entry.getKey(), entry.getValue());
                }
            }
        }

        return results;
    }


}
