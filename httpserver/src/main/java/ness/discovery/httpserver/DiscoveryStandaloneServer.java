package ness.discovery.httpserver;

import ness.discovery.announce.GalaxyAnnouncementModule;
import ness.discovery.announce.ServiceAnnouncementFactory;
import ness.discovery.client.DiscoveryClientModule;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.nesscomputing.config.Config;
import com.nesscomputing.galaxy.GalaxyConfigModule;
import com.nesscomputing.httpserver.HttpServerModule;
import com.nesscomputing.httpserver.standalone.AnnouncingStandaloneServer;
import com.nesscomputing.jackson.NessJacksonModule;
import com.nesscomputing.scopes.threaddelegate.ThreadDelegatedScopeModule;
import com.nesscomputing.tracking.guice.TrackingModule;

/**
 * Service Discovery capable Standalone server. When started, will announce
 */
public abstract class DiscoveryStandaloneServer extends AnnouncingStandaloneServer
{
    @Override
    public Module getPlumbingModules(final Config config)
    {
        final Module standalonePlumbingModule = super.getPlumbingModules(config);
        return new AbstractModule() {
            @Override
            public void configure() {
                install(standalonePlumbingModule);
                install(new GalaxyConfigModule());
                install(new HttpServerModule(config));
                install(new NessJacksonModule());
                install(new ThreadDelegatedScopeModule());
                install(new TrackingModule());
                install(new GalaxyAnnouncementModule(getServiceName(), getServiceType()));
                install(getServiceDiscoveryModule());

                bind (ServiceAnnouncementFactory.class);
            }
        };
    }

    protected Module getServiceDiscoveryModule()
    {
        return new DiscoveryClientModule();
    }

    /**
     * Returns the name of the service to announce. Must be overridden by derived classes.
     */
    protected abstract String getServiceName();

    /**
     * Returns the type of the service to announce.
     */
    protected String getServiceType()
    {
        return null;
    }
}

