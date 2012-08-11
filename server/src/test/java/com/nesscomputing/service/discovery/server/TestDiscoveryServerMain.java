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

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.nesscomputing.config.Config;
import com.nesscomputing.server.StandaloneServer;
import com.nesscomputing.testing.lessio.AllowLocalFileAccess;
import com.nesscomputing.testing.lessio.AllowNetworkAccess;
import com.nesscomputing.testing.lessio.AllowNetworkListen;

@AllowLocalFileAccess(paths={"%TMP_DIR%"})
@AllowNetworkListen(ports={0,12345})
@AllowNetworkAccess(endpoints={"127.0.0.1:12345"})
public class TestDiscoveryServerMain
{
    @Test
    public void testSpinup() throws Exception
    {
        final StandaloneServer server = new DiscoveryServerMain() {
            @Override
            public Config getConfig()
            {
                final File tmpDir = Files.createTempDir();
                tmpDir.deleteOnExit();
                System.setProperty("galaxy.internal.port.http", Integer.toString(findUnusedPort()));
                System.setProperty("ness.zookeeper.dataDir", tmpDir.getAbsolutePath());
                System.setProperty("ness.zookeeper.clientPort", Integer.toString(findUnusedPort()));
                System.setProperty("ness.jmx.enabled", "false");

                int port1 = findUnusedPort();
                int port2 = findUnusedPort();
                System.setProperty("ness.zookeeper.server.1", format("127.0.0.1:%d:%d", port1, port2));

                return Config.getConfig(URI.create("classpath:/config"), "discovery-server");
            }
        };

        Assert.assertFalse(server.isStarted());
        Assert.assertFalse(server.isStopped());

        server.startServer();

        Assert.assertTrue(server.isStarted());
        Assert.assertFalse(server.isStopped());

        server.stopServer();

        Assert.assertTrue(server.isStarted());
        Assert.assertTrue(server.isStopped());
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
                socket.close();
            } catch (IOException ioe) {
                // GNDN
            }
        }

        return port;
    }
}
