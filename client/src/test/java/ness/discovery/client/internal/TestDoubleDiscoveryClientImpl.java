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

import java.net.URI;
import java.util.List;
import java.util.Map;

import ness.discovery.client.DiscoveryClientConfig;
import ness.discovery.client.ServiceInformation;
import ness.discovery.client.internal.DiscoveryClientImpl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class TestDoubleDiscoveryClientImpl
{
    private DiscoveryClientImpl client = null;

    private ServiceInformation userService = null;
    private ServiceInformation userService2 = null;
    private URI uri = null;
    private URI uri2 = null;

    @Before
    public void setUp()
    {
        Assert.assertNull(userService);
        this.userService = ServiceInformation.forService("user-client", "http", "http", "192.168.1.32", 8080);
        this.userService2 = ServiceInformation.forService("user-client", "http", "http", "192.168.1.33", 8080);
        this.uri = URI.create("http://192.168.1.32:8080");
        this.uri2 = URI.create("http://192.168.1.33:8080");

        Assert.assertNull(client);
        this.client = new DiscoveryClientImpl("127.0.0.1:1234", new DiscoveryClientConfig() {}, null);
        this.client.getStateOfTheWorldHolder().setState(ImmutableMap.<String, ConsistentRingGroup> of("user-client", new ConsistentRingGroup(ImmutableList.of(userService, userService2))));

    }

    @After
    public void tearDown()
    {
        Assert.assertNotNull(client);
        this.client = null;
    }

    @Test
    public void testSingleFindService() throws Exception
    {
        final URI serviceUri = client.findServiceUri("user-client", "http");
        Assert.assertNotNull(serviceUri);
        Assert.assertTrue(serviceUri.equals(uri) || serviceUri.equals(uri2));
    }

    @Test
    public void testSeeBothSides() throws Exception
    {
        int result = 0;
        for (int i = 0; i < 100 && result != 3; i++) {
            final URI serviceUri = client.findServiceUri("user-client", "http");
            if (serviceUri.equals(uri)) {
                result |= 1;
            }
            else if (serviceUri.equals(uri2)) {
                result |= 2;
            }
        }
        Assert.assertEquals("Random selection never hit both URIs, something is fishy!", 3, result);
    }


    @Test
    public void testFindAllServiceInformationByType() throws Exception
    {
        final List<ServiceInformation> services = client.findAllServiceInformation("user-client", "http");
        Assert.assertNotNull(services);
        Assert.assertEquals(2, services.size());
    }

    @Test
    public void testFindAllServiceInformation() throws Exception
    {
        final List<ServiceInformation> services = client.findAllServiceInformation("user-client");
        Assert.assertNotNull(services);
        Assert.assertEquals(2, services.size());
    }

    @Test
    public void testFindFullServiceInformation() throws Exception
    {
        final Map<String, List<ServiceInformation>> services = client.findAllServiceInformation();
        Assert.assertNotNull(services);
        Assert.assertEquals(1, services.size());
        final List<ServiceInformation> userServices = services.get("user-client");
        Assert.assertNotNull(userServices);
        Assert.assertEquals(2, userServices.size());
    }
}
