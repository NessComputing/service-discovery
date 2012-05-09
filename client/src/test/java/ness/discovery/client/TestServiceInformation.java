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


