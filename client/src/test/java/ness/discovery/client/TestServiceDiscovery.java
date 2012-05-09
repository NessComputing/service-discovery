package ness.discovery.client;

import static java.lang.String.format;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import ness.discovery.client.internal.DiscoveryClientImpl;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Throwables;
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
