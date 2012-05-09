package ness.discovery.client;

import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class TestServiceInformation
{
    @Test
    public void testSimple()
    {
        final ServiceInformation si = new ServiceInformation("service-name", "service-type", null, null);

        Assert.assertEquals("service-name", si.getServiceName());
        Assert.assertEquals("service-type", si.getServiceType());
        Assert.assertNotNull(si.getServiceId());

        Assert.assertNull(si.getProperty("some-property"));
    }

    @Test
    public void testServiceId()
    {
        final UUID serviceId = UUID.randomUUID();
        final ServiceInformation si = new ServiceInformation("service-name", "service-type", serviceId, null);

        Assert.assertEquals("service-name", si.getServiceName());
        Assert.assertEquals("service-type", si.getServiceType());
        Assert.assertEquals(serviceId, si.getServiceId());

        Assert.assertNull(si.getProperty("some-property"));
    }

    @Test
    public void testProperties()
    {
        final UUID serviceId = UUID.randomUUID();
        final ServiceInformation si = new ServiceInformation("service-name", "service-type", serviceId, ImmutableMap.of("hello", "world",
                                                                                                                   "previous", "next"));

        Assert.assertEquals("service-name", si.getServiceName());
        Assert.assertEquals("service-type", si.getServiceType());
        Assert.assertEquals(serviceId, si.getServiceId());

        Assert.assertNull(si.getProperty("some-property"));
        Assert.assertEquals("world", si.getProperty("hello"));
        Assert.assertEquals("next", si.getProperty("previous"));
    }

    @Test
    public void testImmutable()
    {
        final Map<String, String> props = Maps.newHashMap();
        props.put("hello", "world");
        props.put("previous", "next");

        final UUID serviceId = UUID.randomUUID();
        final ServiceInformation si = new ServiceInformation("service-name", "service-type", serviceId, props);

        Assert.assertEquals("service-name", si.getServiceName());
        Assert.assertEquals("service-type", si.getServiceType());
        Assert.assertEquals(serviceId, si.getServiceId());

        Assert.assertNull(si.getProperty("some-property"));
        Assert.assertEquals("world", si.getProperty("hello"));
        Assert.assertEquals("next", si.getProperty("previous"));

        props.put("hello", "goodbye");
        Assert.assertEquals("world", si.getProperty("hello"));
    }

    @Test
    public void testService()
    {
        final ServiceInformation si = ServiceInformation.forService("user-service", "http", "http", "192.168.1.32", 80);

        Assert.assertEquals("user-service", si.getServiceName());
        Assert.assertEquals("http", si.getServiceType());
        Assert.assertNotNull(si.getServiceId());

        Assert.assertEquals("192.168.1.32", si.getProperty(ServiceInformation.PROP_SERVICE_ADDRESS));
        Assert.assertEquals(80, Integer.parseInt(si.getProperty(ServiceInformation.PROP_SERVICE_PORT)));
    }
}


