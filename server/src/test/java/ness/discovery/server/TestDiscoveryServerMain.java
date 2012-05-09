package ness.discovery.server;

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
import com.nesscomputing.httpserver.standalone.StandaloneServer;
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
