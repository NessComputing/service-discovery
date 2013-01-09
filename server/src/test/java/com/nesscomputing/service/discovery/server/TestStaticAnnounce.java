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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import com.google.common.net.HttpHeaders;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Stage;

import org.apache.commons.configuration.SystemConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.nesscomputing.config.Config;
import com.nesscomputing.config.ConfigModule;
import com.nesscomputing.httpclient.HttpClient;
import com.nesscomputing.httpclient.guice.HttpClientModule;
import com.nesscomputing.httpclient.response.ContentResponseHandler;
import com.nesscomputing.httpclient.response.HttpResponse;
import com.nesscomputing.httpclient.response.HttpResponseContentConverter;
import com.nesscomputing.httpclient.response.JsonContentConverter;
import com.nesscomputing.httpclient.response.Valid2xxContentConverter;
import com.nesscomputing.jackson.NessJacksonModule;
import com.nesscomputing.lifecycle.Lifecycle;
import com.nesscomputing.lifecycle.LifecycleStage;
import com.nesscomputing.lifecycle.ServiceDiscoveryLifecycle;
import com.nesscomputing.lifecycle.guice.LifecycleModule;
import com.nesscomputing.logging.Log;
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
public class TestStaticAnnounce
{
    private static final Log LOG = Log.findLog();
    StandaloneServer server;
    String serverBase;

    @Inject
    Lifecycle lifecycle;

    @Inject
    ReadOnlyDiscoveryClient discoveryClient;

    @Inject
    HttpClient httpClient;

    @Inject
    ObjectMapper objectMapper;

    @Before
    public void spinup() throws Exception
    {
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

                return Config.getConfig(URI.create("classpath:/config"), "discovery-server");
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
                install (new HttpClientModule());
                install (new DiscoveryClientModule(true));
            }
        }).injectMembers(this);

        lifecycle.executeTo(LifecycleStage.START_STAGE);
    }

    @Test
    public void testStaticAnnounce() throws Exception
    {
        final String announceBase = serverBase +  "/announcement";

        assertTrue(discoveryClient.findAllServiceInformation().isEmpty());

        final TypeReference<Map<String, List<ServiceInformation>>> responseType = new TypeReference<Map<String, List<ServiceInformation>>>() {};

        Map<String, List<ServiceInformation>> announcements = httpClient.get(announceBase,
                JsonContentConverter.getResponseHandler(responseType, objectMapper))
                .perform();

        assertTrue(announcements.get("results").isEmpty());

        final ContentResponseHandler<HttpResponse> handler = ContentResponseHandler.forConverter(new HttpResponseContentConverter());

        final HttpResponse response = httpClient.post(announceBase, handler)
            .setContentType(MediaType.APPLICATION_JSON)
            .setContent(objectMapper.writeValueAsBytes(ImmutableMap.of(
                    "name", "testing",
                    "scheme", "http",
                    "address", "localhost",
                    "port", 8080
            ))).perform();

        assertEquals(response.getBodyAsString(), 201, response.getStatusCode());

        int tries = 20;
        ServiceInformation si = null;

        while (true) {
            try {
                si = discoveryClient.findServiceInformation("testing", null);
                break;
            } catch (final ServiceNotAvailableException e) {
            }

            if (tries-- == 0) {
                fail("Announcement never showed up");
            }

            Thread.sleep(100); // Ensure that we get some ZK ticks
        }
        LOG.info("=== Found 'testing'");
        assertNotNull(si);
        assertTrue(si.isStaticAnnouncement());
        assertEquals("8080", si.getProperty(ServiceInformation.PROP_SERVICE_PORT));
        final String location = Iterables.getOnlyElement(response.getHeaders().get(HttpHeaders.LOCATION));
        assertTrue(location.endsWith(si.getServiceId().toString()));

        Thread.sleep(500); // The DiscoveryClient in the server takes yet more time to catch up
        LOG.info("=== Read HTTP services");

        announcements = httpClient.get(announceBase,
                JsonContentConverter.getResponseHandler(responseType, objectMapper))
                .perform();

        assertEquals(ImmutableMap.of("results", ImmutableList.of(si)), announcements);

        httpClient.delete(location, Valid2xxContentConverter.DEFAULT_FAILING_RESPONSE_HANDLER).perform();

        tries = 20;
        while (true) {
            if (discoveryClient.findAllServiceInformation().isEmpty()) {
                break;
            }
            tries--;
            if (tries-- == 0) {
                fail("Announcement never disappeared");
            }

            Thread.sleep(100); // Ensure that we get some ZK ticks
        }
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
