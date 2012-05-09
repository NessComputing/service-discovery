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

public class TestDiscoveryClientImpl
{
    private DiscoveryClientImpl client = null;

    @Before
    public void setUp()
    {
        ServiceInformation httpUserService1 = ServiceInformation.forService("user-client", "http", "http", "192.168.1.32", 8080);
        ServiceInformation httpUserService2 = ServiceInformation.forService("user-client", "http", "http", "192.168.1.33", 8080);
        ServiceInformation httpsUserService1 = ServiceInformation.forService("user-client", "https", "https", "192.168.1.32", 8443);
        ServiceInformation httpsUserService2 = ServiceInformation.forService("user-client", "https", "https", "192.168.1.33", 8443);
        ServiceInformation httpPlaceService1 = ServiceInformation.forService("place-client", "http", "http", "192.168.1.34", 8080);
        ServiceInformation httpPlaceService2 = ServiceInformation.forService("place-client", "http", "http", "192.168.1.35", 8080);
        ServiceInformation httpsPlaceService1 = ServiceInformation.forService("place-client", "https", "https", "192.168.1.36", 8443);
        ServiceInformation httpsPlaceService2 = ServiceInformation.forService("place-client", "https", "https", "192.168.1.37", 8443);

        Assert.assertNull(client);
        this.client = new DiscoveryClientImpl("127.0.0.1:1234", new DiscoveryClientConfig() {}, null);
        this.client.getStateOfTheWorldHolder().setState(ImmutableMap.<String, ConsistentRingGroup> of(
        		"user-client", new ConsistentRingGroup(ImmutableList.of(httpUserService1, httpsUserService1, httpUserService2, httpsUserService2)),
        		"place-client", new ConsistentRingGroup(ImmutableList.of(httpPlaceService1, httpsPlaceService1, httpPlaceService2, httpsPlaceService2))));

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
        Assert.assertNotNull(client.findServiceUri("user-client", "http"));
        Assert.assertNotNull(client.findServiceUri("user-client", "https"));
        Assert.assertNotNull(client.findServiceUri("place-client", "http"));
        Assert.assertNotNull(client.findServiceUri("place-client", "https"));
    }

    @Test
    public void testSingleFindServiceInformation() throws Exception
    {
        final ServiceInformation httpUserService = client.findServiceInformation("user-client", "http");
        Assert.assertEquals("user-client", httpUserService.getServiceName());
        Assert.assertEquals("http", httpUserService.getServiceType());

        final ServiceInformation httpsUserService = client.findServiceInformation("user-client", "https");
        Assert.assertEquals("user-client", httpsUserService.getServiceName());
        Assert.assertEquals("https", httpsUserService.getServiceType());

        final ServiceInformation httpPlaceService = client.findServiceInformation("place-client", "http");
        Assert.assertEquals("place-client", httpPlaceService.getServiceName());
        Assert.assertEquals("http", httpPlaceService.getServiceType());

        final ServiceInformation httpsPlaceService = client.findServiceInformation("place-client", "https");
        Assert.assertEquals("place-client", httpsPlaceService.getServiceName());
        Assert.assertEquals("https", httpsPlaceService.getServiceType());

    }

    @Test
    public void testFindAllServiceInformationByType() throws Exception
    {
        final List<ServiceInformation> httpUserServices = client.findAllServiceInformation("user-client", "http");
        Assert.assertNotNull(httpUserServices);
        Assert.assertEquals(2, httpUserServices.size());

        final List<ServiceInformation> httpsUserServices = client.findAllServiceInformation("user-client", "https");
        Assert.assertNotNull(httpsUserServices);
        Assert.assertEquals(2, httpsUserServices.size());

        final List<ServiceInformation> httpPlaceServices = client.findAllServiceInformation("place-client", "http");
        Assert.assertNotNull(httpPlaceServices);
        Assert.assertEquals(2, httpPlaceServices.size());

        final List<ServiceInformation> httpsPlaceServices = client.findAllServiceInformation("place-client", "https");
        Assert.assertNotNull(httpsPlaceServices);
        Assert.assertEquals(2, httpsPlaceServices.size());
    }

    @Test
    public void testFindAllServiceInformation() throws Exception
    {
        final List<ServiceInformation> userServices = client.findAllServiceInformation("user-client");
        Assert.assertNotNull(userServices);
        Assert.assertEquals(4, userServices.size());

        final List<ServiceInformation> placeServices = client.findAllServiceInformation("place-client");
        Assert.assertNotNull(placeServices);
        Assert.assertEquals(4, placeServices.size());
    }

    @Test
    public void testFindFullServiceInformation() throws Exception
    {
        final Map<String, List<ServiceInformation>> services = client.findAllServiceInformation();
        Assert.assertNotNull(services);
        Assert.assertEquals(2, services.size());
        final List<ServiceInformation> userServices = services.get("user-client");
        Assert.assertNotNull(userServices);
        Assert.assertEquals(4, userServices.size());
        final List<ServiceInformation> placeServices = services.get("place-client");
        Assert.assertNotNull(placeServices);
        Assert.assertEquals(4, placeServices.size());
    }
}
