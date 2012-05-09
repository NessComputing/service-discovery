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
package com.nesscomputing.service.discovery.httpserver;


import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.nesscomputing.config.Config;
import com.nesscomputing.galaxy.GalaxyConfigModule;
import com.nesscomputing.httpserver.HttpServerModule;
import com.nesscomputing.httpserver.standalone.AnnouncingStandaloneServer;
import com.nesscomputing.jackson.NessJacksonModule;
import com.nesscomputing.scopes.threaddelegate.ThreadDelegatedScopeModule;
import com.nesscomputing.service.discovery.announce.GalaxyAnnouncementModule;
import com.nesscomputing.service.discovery.announce.ServiceAnnouncementFactory;
import com.nesscomputing.service.discovery.client.DiscoveryClientModule;
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

