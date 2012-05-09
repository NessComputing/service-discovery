package ness.discovery.client.internal;

import java.util.List;
import java.util.Map;

import ness.discovery.client.DiscoveryClientConfig;
import ness.discovery.client.ServiceInformation;
import ness.discovery.client.ServiceNotAvailableException;
import ness.discovery.client.internal.DiscoveryClientImpl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestEmptyDiscoveryClientImpl
{
    private DiscoveryClientImpl client = null;

    @Before
    public void setUp()
    {
        Assert.assertNull(client);
        this.client = new DiscoveryClientImpl("127.0.0.1:1234", new DiscoveryClientConfig() {}, null);
    }

    @After
    public void tearDown()
    {
        Assert.assertNotNull(client);
        this.client = null;
    }

    @Test(expected=ServiceNotAvailableException.class)
    public void testFindService() throws Exception
    {
        client.findServiceUri("user-client", "http");
    }

    @Test(expected=ServiceNotAvailableException.class)
    public void testFindServiceInformation() throws Exception
    {
        client.findServiceInformation("user-client", "http");
    }

    @Test
    public void testFindAllServiceInformationByType() throws Exception
    {
        final List<ServiceInformation> services = client.findAllServiceInformation("user-client", "http");
        Assert.assertNotNull(services);
        Assert.assertEquals(0, services.size());
    }

    @Test
    public void testFindAllServiceInformation() throws Exception
    {
        final List<ServiceInformation> services = client.findAllServiceInformation("user-client");
        Assert.assertNotNull(services);
        Assert.assertEquals(0, services.size());
    }

    @Test
    public void testFindFullServiceInformation() throws Exception
    {
        final Map<String, List<ServiceInformation>> services = client.findAllServiceInformation();
        Assert.assertNotNull(services);
        Assert.assertEquals(0, services.size());
    }
}
