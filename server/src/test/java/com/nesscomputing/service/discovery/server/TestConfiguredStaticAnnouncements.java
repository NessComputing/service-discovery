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

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.util.Map;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Stage;

import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.nesscomputing.config.Config;
import com.nesscomputing.config.ConfigModule;
import com.nesscomputing.jackson.NessJacksonModule;
import com.nesscomputing.lifecycle.Lifecycle;
import com.nesscomputing.lifecycle.LifecycleStage;
import com.nesscomputing.lifecycle.ServiceDiscoveryLifecycle;
import com.nesscomputing.lifecycle.guice.LifecycleModule;
import com.nesscomputing.server.StandaloneServer;
import com.nesscomputing.service.discovery.client.DiscoveryClientModule;
import com.nesscomputing.service.discovery.client.ReadOnlyDiscoveryClient;
import com.nesscomputing.service.discovery.client.ServiceInformation;
import com.nesscomputing.service.discovery.client.ServiceNotAvailableException;
import com.nesscomputing.testing.lessio.AllowLocalFileAccess;
import com.nesscomputing.testing.lessio.AllowNetworkAccess;
import com.nesscomputing.testing.lessio.AllowNetworkListen;

@AllowLocalFileAccess(paths={"%TMP_DIR%"})
@AllowNetworkListen(ports={0})
@AllowNetworkAccess(endpoints= {"127.0.0.1:0"})
public class TestConfiguredStaticAnnouncements
{
    StandaloneServer server;
    String serverBase;

    @Inject
    Lifecycle lifecycle;

    @Inject
    ReadOnlyDiscoveryClient discoveryClient;

    @Before
    public void spinup() throws Exception
    {
        final Map<String, String> announceConfig = ImmutableMap.of(
                "ness.discovery.static-announce.65f57132-8415-422e-b44a-7543dda54858.name", "foo",
                "ness.discovery.static-announce.65f57132-8415-422e-b44a-7543dda54858.scheme", "http",
                "ness.discovery.static-announce.65f57132-8415-422e-b44a-7543dda54858.address", "127.0.0.1",
                "ness.discovery.static-announce.65f57132-8415-422e-b44a-7543dda54858.port", "12345",
                "ness.discovery.static-announce.65f57132-8415-422e-b44a-7543dda54858.type", "bar"
            );

        System.setProperty("ness.discovery.enabled", "true");
        server = new DiscoveryServerMain() {
            @Override
            public Config getConfig()
            {
                final File tmpDir = Files.createTempDir();
                tmpDir.deleteOnExit();
                final String httpPort = Integer.toString(findUnusedPort());

                serverBase = "http://localhost:" + httpPort;

                System.setProperty("galaxy.internal.port.http", httpPort);
                System.setProperty("ness.zookeeper.dataDir", tmpDir.getAbsolutePath());
                System.setProperty("ness.zookeeper.clientPort", Integer.toString(findUnusedPort()));
                System.setProperty("ness.jmx.enabled", "false");

                final int port1 = findUnusedPort();
                final int port2 = findUnusedPort();
                System.setProperty("ness.zookeeper.server.1", format("127.0.0.1:%d:%d", port1, port2));
                System.setProperty("ness.zookeeper.clientPortAddress", "127.0.0.1");

                return Config.getOverriddenConfig(Config.getConfig(URI.create("classpath:/config"), "discovery-server"),
                        new MapConfiguration(announceConfig));
            }
        };

        server.startServer();

        Guice.createInjector(Stage.PRODUCTION, new AbstractModule() {
            @Override
            protected void configure()
            {
                binder().requireExplicitBindings();
                binder().disableCircularProxies();

                install (new ConfigModule(Config.getFixedConfig(new SystemConfiguration())));
                install (new LifecycleModule(ServiceDiscoveryLifecycle.class));
                install (new NessJacksonModule());
                install (new DiscoveryClientModule(true));
            }
        }).injectMembers(this);

        lifecycle.executeTo(LifecycleStage.START_STAGE);
    }

    @Test
    public void testStaticAnnounce() throws Exception
    {
        discoveryClient.waitForWorldChange();

        ServiceInformation si = null;

        int tries = 20;

        while (true) {
            try {
                si = discoveryClient.findServiceInformation("foo", "bar");
                break;
            } catch (final ServiceNotAvailableException e) {
            }

            if (tries-- == 0) {
                fail("Service never appeared");
            }

            Thread.sleep(100);
        }

        assertEquals("bar", si.getServiceType());
        assertEquals("http", si.getProperty(ServiceInformation.PROP_SERVICE_SCHEME));
        assertEquals("127.0.0.1", si.getProperty(ServiceInformation.PROP_SERVICE_ADDRESS));
        assertEquals("12345", si.getProperty(ServiceInformation.PROP_SERVICE_PORT));
    }

    @After
    public void shutdown() {
        if (lifecycle != null) {
            lifecycle.executeTo(LifecycleStage.STOP_STAGE);
        }

        if (server !=null) {
            server.stopServer();
        }
    }

    private static final int findUnusedPort()
    {
        int port;

        ServerSocket socket = null;
        try {
            socket = new ServerSocket();
            socket.bind(new InetSocketAddress(0));
            port = socket.getLocalPort();
        }
        catch (final IOException ioe) {
            throw Throwables.propagate(ioe);
        }
        finally {
            try {
                socket.close();
            } catch (final IOException ioe) {
                // GNDN
            }
        }

        return port;
    }
}
