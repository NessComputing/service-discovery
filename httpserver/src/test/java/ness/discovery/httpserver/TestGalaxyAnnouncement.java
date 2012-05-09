package ness.discovery.httpserver;

import java.net.InetSocketAddress;
import java.util.Map;

import ness.discovery.announce.GalaxyAnnouncementModule;
import ness.discovery.announce.GalaxyAnnouncer;
import ness.discovery.client.DiscoveryClientModule;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Modules;
import com.nesscomputing.config.Config;
import com.nesscomputing.config.ConfigModule;
import com.nesscomputing.httpserver.HttpServerModule;
import com.nesscomputing.jmx.JmxModule;
import com.nesscomputing.lifecycle.ServiceDiscoveryLifecycle;
import com.nesscomputing.lifecycle.guice.LifecycleModule;

public class TestGalaxyAnnouncement
{
    @Test
    public void testSpinup()
    {
        final Config config = Config.getEmptyConfig();

        final Injector inj = Guice.createInjector(Stage.PRODUCTION,
                                                  new ConfigModule(config),
                                                  new HttpServerModule(config),
                                                  new JmxModule(),
                                                  new LifecycleModule(ServiceDiscoveryLifecycle.class),
                                                  Modules.override(new DiscoveryClientModule()).with(new AbstractModule() {

                                                    @Override
                                                    public void configure()
                                                    {
                                                        bindConstant().annotatedWith(DiscoveryClientModule.ZOOKEEPER_CONNECT_NAMED).to("1.1.1.1:2345");
                                                        bind(new TypeLiteral<Map<Integer, InetSocketAddress>>() {}).annotatedWith(DiscoveryClientModule.ZOOKEEPER_CONNECT_NAMED).toInstance(ImmutableMap.of(1, new InetSocketAddress("1.1.1.1", 2345)));
                                                    }

                                                  }),
                                                  new GalaxyAnnouncementModule("my-service", "my-type"));

        Assert.assertNotNull(inj);

        final GalaxyAnnouncer announcer = inj.getInstance(GalaxyAnnouncer.class);
        Assert.assertNotNull(announcer);
    }
}
