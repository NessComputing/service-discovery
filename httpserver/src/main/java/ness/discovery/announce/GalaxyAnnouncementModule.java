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
package ness.discovery.announce;

import org.apache.commons.lang3.ObjectUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.nesscomputing.config.Config;

/**
 * Install the Galaxy Announcer to automatically services from the galaxy config to service discovery.
 */
public class GalaxyAnnouncementModule extends AbstractModule
{
    private final String serviceName;
    private final String serviceType;

    public GalaxyAnnouncementModule()
    {
        this(null, null);
    }

    public GalaxyAnnouncementModule(final String serviceName, final String serviceType)
    {
        this.serviceName = serviceName;
        this.serviceType = serviceType;
    }

    @Override
    public void configure()
    {
        bind(GalaxyAnnouncer.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    AnnouncementConfig getAnnouncementConfig(final Config config)
    {
        final AnnouncementConfig baseConfig = config.getBean(AnnouncementConfig.class);

        return new AnnouncementConfig(baseConfig) {
            @Override
            public String getServiceName() {
                return ObjectUtils.toString(super.getServiceName(), serviceName);
            }

            @Override
            public String getServiceType() {
                return ObjectUtils.toString(super.getServiceType(), serviceType);
            }
        };
    }
}
