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

import org.junit.Assert;
import org.junit.Test;

public class TestServiceURI
{
    @Test
    public void testBasicUri() throws Exception
    {
        serviceTest(new ServiceURI("srvc://follow/v1/resource"), "basic with path", "follow", null, "/v1/resource", null, null);
        serviceTest(new ServiceURI("srvc://follow/v1/resource?query"), "basic with path and query", "follow", null, "/v1/resource", "query", null);
        serviceTest(new ServiceURI("srvc://follow/v1/resource?query#fragment"), "base with path, query and fragment", "follow", null, "/v1/resource", "query", "fragment");
    }

    @Test
    public void testServiceUri() throws Exception
    {
        serviceTest(new ServiceURI("srvc://follow:test/v1/resource"), "type with path", "follow", "test", "/v1/resource", null, null);
        serviceTest(new ServiceURI("srvc://follow:test/v1/resource?query"), "type with path and query", "follow", "test", "/v1/resource", "query", null);
        serviceTest(new ServiceURI("srvc://follow:test/v1/resource?query#fragment"), "type with path, query and fragment", "follow", "test", "/v1/resource", "query", "fragment");
    }

    @Test
    public void testSpecials() throws Exception
    {
        serviceTest(new ServiceURI("srvc://follow:test/?foo=bar"), "no path uri",  "follow", "test", "/", "foo=bar", null);
        serviceTest(new ServiceURI("srvc://follow/#baz"), "just fragment",  "follow", null, "/", null, "baz");
    }

    protected void serviceTest(final ServiceURI serviceUri,
                               final String msg,
                               final String serviceName,
                               final String serviceType,
                               final String path,
                               final String query,
                               final String fragment)
    {
        Assert.assertNotNull(msg, serviceUri);
        Assert.assertNotNull(msg, serviceUri.getServiceName());
        Assert.assertEquals(msg, serviceName, serviceUri.getServiceName());

        if (serviceType == null) {
            Assert.assertNull(msg, serviceUri.getServiceType());
        }
        else {
            Assert.assertEquals(msg, serviceType, serviceUri.getServiceType());
        }

        if (path == null) {
            Assert.assertNull(msg, serviceUri.getPath());
        }
        else {
            Assert.assertEquals(msg, path, serviceUri.getPath());
        }

        if (query == null) {
            Assert.assertNull(msg, serviceUri.getQuery());
        }
        else {
            Assert.assertEquals(msg, query, serviceUri.getQuery());
        }

        if (fragment == null) {
            Assert.assertNull(msg, serviceUri.getFragment());
        }
        else {
            Assert.assertEquals(msg, fragment, serviceUri.getFragment());
        }
    }
}
