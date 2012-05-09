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

import static java.lang.String.format;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.nesscomputing.config.Config;
import com.nesscomputing.jackson.NessJacksonModule;

public class TestDiscoveryClientModule
{
    @Test
    public void testMultiServer()
    {
        final Config config = Config.getConfig(URI.create("classpath:/test-config"), "discovery");
        final Injector injector = Guice.createInjector(
                       new Module() {
                           @Override
                           public void configure(final Binder binder) {
                               binder.requireExplicitBindings();
                               binder.disableCircularProxies();
                               binder.bind(Config.class).toInstance(config);
                           }
                       },
                       new NessJacksonModule(),
                       new DiscoveryClientModule());

        final Map<Integer, InetSocketAddress> servers = injector.getInstance(Key.get(new TypeLiteral<Map<Integer, InetSocketAddress>>() {}, DiscoveryClientModule.ZOOKEEPER_CONNECT_NAMED));

        Assert.assertNotNull(servers);

        Assert.assertEquals(4, servers.size());

        for (int i=1; i<5;i++)
        {
            final InetSocketAddress address = servers.get(i);
            Assert.assertNotNull(address);
            Assert.assertEquals(format("%d.%d.%d.%d", i, i, i, i), address.getAddress().getHostAddress());
            Assert.assertEquals(4815, address.getPort());
        }
    }

    @Test
    public void testSingleServer()
    {
        final Config config = Config.getConfig(URI.create("classpath:/test-config"), "discovery-single");
        final Injector injector = Guice.createInjector(
                       new Module() {
                           @Override
                           public void configure(final Binder binder) {
                               binder.requireExplicitBindings();
                               binder.disableCircularProxies();
                               binder.bind(Config.class).toInstance(config);
                           }
                       },
                       new NessJacksonModule(),
                       new DiscoveryClientModule());

        final Map<Integer, InetSocketAddress> servers = injector.getInstance(Key.get(new TypeLiteral<Map<Integer, InetSocketAddress>>() {}, DiscoveryClientModule.ZOOKEEPER_CONNECT_NAMED));

        Assert.assertNotNull(servers);

        Assert.assertEquals(1, servers.size());
        final InetSocketAddress address = servers.get(1);
        Assert.assertNotNull(address);
        Assert.assertEquals("4.8.15.16", address.getAddress().getHostAddress());
        Assert.assertEquals(1516, address.getPort());
    }
}
