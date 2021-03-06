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
package com.nesscomputing.service.discovery.client.internal;

import java.net.URI;
import java.util.List;
import java.util.Map;


import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.nesscomputing.service.discovery.client.DiscoveryClientConfig;
import com.nesscomputing.service.discovery.client.ServiceInformation;
import com.nesscomputing.service.discovery.client.ServiceNotAvailableException;
import com.nesscomputing.service.discovery.client.internal.ConsistentRingGroup;
import com.nesscomputing.service.discovery.client.internal.DiscoveryClientImpl;

/**
 * Tests with a single service in the map.
 */
public class TestSingleDiscoveryClientImpl
{
    private DiscoveryClientImpl client = null;

    private ServiceInformation userService = null;

    @Before
    public void setUp()
    {
        Assert.assertNull(userService);
        this.userService = ServiceInformation.forService("user-client", "http", "http", "192.168.1.32", 8080);

        Assert.assertNull(client);
        this.client = new DiscoveryClientImpl("127.0.0.1:1234", new DiscoveryClientConfig() {}, null);
        this.client.getStateOfTheWorldHolder().setState(ImmutableMap.<String, ConsistentRingGroup> of("user-client", new ConsistentRingGroup(ImmutableList.of(userService))));

    }

    @After
    public void tearDown()
    {
        Assert.assertNotNull(client);
        this.client = null;
    }

    @Test(expected=ServiceNotAvailableException.class)
    public void testFindServiceWithoutPort() throws Exception
    {
        final ServiceInformation noPortService = new ServiceInformation("user-client", "http", null, ImmutableMap.of(ServiceInformation.PROP_SERVICE_ADDRESS, "192.168.1.32"));
        this.client.getStateOfTheWorldHolder().setState(ImmutableMap.<String, ConsistentRingGroup> of("user-client", new ConsistentRingGroup(ImmutableList.of(noPortService))));
        client.findServiceUri("user-client", "http");
    }

    @Test(expected=ServiceNotAvailableException.class)
    public void testFindServiceWithoutAddress() throws Exception
    {
        final ServiceInformation noAddressService = new ServiceInformation("user-client", "http", null, ImmutableMap.of(ServiceInformation.PROP_SERVICE_PORT, "8080"));
        this.client.getStateOfTheWorldHolder().setState(ImmutableMap.<String, ConsistentRingGroup> of("user-client", new ConsistentRingGroup(ImmutableList.of(noAddressService))));
        client.findServiceUri("user-client", "http");
    }

    @Test(expected=ServiceNotAvailableException.class)
    public void testIgnoreOtherServiceType() throws Exception
    {
        client.findServiceUri("user-client", "https");
    }

    @Test
    public void testSingleFindService() throws Exception
    {
        final URI serviceUri = client.findServiceUri("user-client", "http");
        Assert.assertNotNull(serviceUri);
        Assert.assertEquals(URI.create("http://192.168.1.32:8080"), serviceUri);
    }


    @Test
    public void testFindServiceInformation() throws Exception
    {
        final ServiceInformation si = client.findServiceInformation("user-client", "http");
        Assert.assertEquals(userService, si);
    }

    @Test
    public void testFindAllServiceInformationByType() throws Exception
    {
        final List<ServiceInformation> services = client.findAllServiceInformation("user-client", "http");
        Assert.assertNotNull(services);
        Assert.assertEquals(1, services.size());
        Assert.assertEquals(userService, services.get(0));
    }

    @Test
    public void testFindAllServiceInformation() throws Exception
    {
        final List<ServiceInformation> services = client.findAllServiceInformation("user-client");
        Assert.assertNotNull(services);
        Assert.assertEquals(1, services.size());
        Assert.assertEquals(userService, services.get(0));
    }

    @Test
    public void testFindFullServiceInformation() throws Exception
    {
        final Map<String, List<ServiceInformation>> services = client.findAllServiceInformation();
        Assert.assertNotNull(services);
        Assert.assertEquals(1, services.size());
        final List<ServiceInformation> userServices = services.get("user-client");
        Assert.assertNotNull(userServices);
        Assert.assertEquals(userService, userServices.get(0));
    }
}
