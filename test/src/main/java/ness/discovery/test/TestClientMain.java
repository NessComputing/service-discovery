package ness.discovery.test;

import java.util.Random;

import ness.discovery.client.DiscoveryClient;
import ness.discovery.client.ServiceInformation;
import ness.discovery.httpserver.DiscoveryStandaloneServer;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.nesscomputing.config.Config;
import com.nesscomputing.galaxy.GalaxyConfig;
import com.nesscomputing.httpserver.standalone.StandaloneServer;
import com.nesscomputing.jmx.starter.guice.JmxStarterModule;
import com.nesscomputing.logging.Log;

/**
 * Test service that announces a service and then toggles another at random.
 */
public class TestClientMain extends DiscoveryStandaloneServer
{
    private static final Log LOG = Log.findLog();

    @Inject
    private GalaxyConfig galaxyConfig;

    @Inject
    private DiscoveryClient discoveryClient;

    public static void main(final String [] args) throws Exception
    {
        final StandaloneServer server = new TestClientMain();
        server.startServer();
    }

    @Override
    public void startServer()
    {
        final Random random = new Random();

        super.startServer();

        Thread t = new Thread(new Runnable() {

            boolean toggle = false;

            @Override
            public void run()
            {
                final ServiceInformation toggleService = ServiceInformation.forService("test-client", "toggle", "http", galaxyConfig.getInternalIp().getIp(), galaxyConfig.getInternalIp().getHttpPort());
                try {
                    while (true) {
                        toggle = !toggle;
                        if (toggle) {
                            LOG.info("Service visible!");
                            discoveryClient.announce(toggleService);
                        }
                        else {
                            LOG.info("Service invisible!");
                            discoveryClient.unannounce(toggleService);
                        }
                        Thread.sleep(1000L + random.nextInt(1000));
                    }
                }
                catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "toggle-thread");
        t.start();
    }

    @Override
    protected String getServiceName()
    {
        return "test-service";
    }

    @Override
    public Module getMainModule(final Config config)
    {
        return new AbstractModule() {
            @Override
            public void configure()
            {
                install(new JmxStarterModule(config));
            }
        };
    }
}
