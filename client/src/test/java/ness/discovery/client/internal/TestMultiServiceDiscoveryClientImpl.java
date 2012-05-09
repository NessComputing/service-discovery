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

public class TestMultiServiceDiscoveryClientImpl
{
    private DiscoveryClientImpl client = null;

    private ServiceInformation httpUserService = null;
    private ServiceInformation httpsUserService = null;
    private URI httpUri = null;
    private URI httpsUri = null;

    @Before
    public void setUp()
    {
        Assert.assertNull(httpUserService);
        this.httpUserService = ServiceInformation.forService("user-client", "http", "http", "192.168.1.32", 8080);
        this.httpsUserService = ServiceInformation.forService("user-client", "https", "https", "192.168.1.33", 8080);
        this.httpUri = URI.create("http://192.168.1.32:8080");
        this.httpsUri = URI.create("https://192.168.1.33:8080");

        Assert.assertNull(client);
        this.client = new DiscoveryClientImpl("127.0.0.1:1234", new DiscoveryClientConfig() {}, null);
        this.client.getStateOfTheWorldHolder().setState(ImmutableMap.<String, ConsistentRingGroup> of("user-client", new ConsistentRingGroup(ImmutableList.of(httpUserService, httpsUserService))));

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
        Assert.assertEquals(httpUri, serviceUri);

        final URI serviceUri2 = client.findServiceUri("user-client", "https");
        Assert.assertNotNull(serviceUri2);
        Assert.assertEquals(httpsUri, serviceUri2);
    }

    @Test
    public void testFindAllServiceInformationByType() throws Exception
    {
        final List<ServiceInformation> httpServices = client.findAllServiceInformation("user-client", "http");
        Assert.assertNotNull(httpServices);
        Assert.assertEquals(1, httpServices.size());

        final List<ServiceInformation> httpsServices = client.findAllServiceInformation("user-client", "https");
        Assert.assertNotNull(httpsServices);
        Assert.assertEquals(1, httpsServices.size());
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
        final List<ServiceInformation> httpUserServices = services.get("user-client");
        Assert.assertNotNull(httpUserServices);
        Assert.assertEquals(2, httpUserServices.size());
    }
}
