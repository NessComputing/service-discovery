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
package com.nesscomputing.service.discovery.client;

import static java.lang.String.format;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;


import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Throwables;
import com.nesscomputing.service.discovery.client.DiscoveryClientConfig;
import com.nesscomputing.service.discovery.client.internal.DiscoveryClientImpl;
import com.nesscomputing.testing.lessio.AllowNetworkListen;

@AllowNetworkListen(ports={0})
public class TestServiceDiscovery
{
    @Test
    public void testDisabled() throws Exception
    {
        final DiscoveryClientConfig config = new DiscoveryClientConfig() {};
        Assert.assertFalse(config.isEnabled());
        final DiscoveryClientImpl client = new DiscoveryClientImpl(format("127.0.0.1:%d", findUnusedPort()), config, new ObjectMapper());
        client.start();
        Thread.sleep(2000L);
        client.stop();
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
        catch (IOException ioe) {
            throw Throwables.propagate(ioe);
        }
        finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException ioe) {
                // GNDN
            }
        }

        return port;
    }

}
