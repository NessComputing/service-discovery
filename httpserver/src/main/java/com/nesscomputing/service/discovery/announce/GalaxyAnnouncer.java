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
package com.nesscomputing.service.discovery.announce;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.nesscomputing.galaxy.GalaxyConfig;
import com.nesscomputing.galaxy.GalaxyIp;
import com.nesscomputing.httpserver.GalaxyHttpServerConfig;
import com.nesscomputing.logging.Log;
import com.nesscomputing.service.discovery.client.DiscoveryClient;
import com.nesscomputing.service.discovery.client.ServiceAnnouncer;
import com.nesscomputing.service.discovery.client.ServiceInformation;

/**
 * Announces HTTP and HTTPS services based off the Galaxy configuration and
 * the state of the HttpServer Configuration.
 */
public class GalaxyAnnouncer extends ServiceAnnouncer
{
    private static final Log LOG = Log.findLog();

    @Inject
    GalaxyAnnouncer(final DiscoveryClient discoveryClient,
                    final AnnouncementConfig announcementConfig,
                    final GalaxyHttpServerConfig httpServerConfig,
                    final GalaxyConfig galaxyConfig)
    {
        super(discoveryClient);
        setServices(GalaxyAnnouncer.buildServices(announcementConfig, httpServerConfig, galaxyConfig));
    }

    public static final Set<ServiceInformation> buildServices(final AnnouncementConfig announcementConfig,
                                                              final GalaxyHttpServerConfig httpServerConfig,
                                                              final GalaxyConfig galaxyConfig)
    {
        final Set<ServiceInformation> services = Sets.newHashSet();

        final String serviceName = announcementConfig.getServiceName();
        if (StringUtils.isBlank(serviceName)) {
            LOG.warn("No service name given, not announcing anything. This is not what you want!");
        }
        else {
            final String serviceType = announcementConfig.getServiceType();

            if (announcementConfig.isAnnounceInternal()) {
                LOG.debug("Internal services are announced");
                services.addAll(buildInternalServices(serviceName, serviceType, httpServerConfig, galaxyConfig));
            }
            if (announcementConfig.isAnnounceExternal()) {
                LOG.debug("External services are announced");
                services.addAll(buildExternalServices(serviceName, serviceType, httpServerConfig, galaxyConfig));
            }
        }

        LOG.debug("Total number of announcements: %d", services.size());
        return services;
    }

    public static final Set<ServiceInformation> buildInternalServices(final String serviceName,
                                                                      final String serviceType,
                                                                      final GalaxyHttpServerConfig httpServerConfig,
                                                                      final GalaxyConfig galaxyConfig)
    {
        final Set<ServiceInformation> services = Sets.newHashSet();

        final GalaxyIp internalIp = galaxyConfig.getInternalIp();
        if (httpServerConfig.isInternalHttpEnabled()) {
            services.add(ServiceInformation.forService(serviceName, serviceType, "http", internalIp.getIp(), internalIp.getHttpPort()));
        }
        if (httpServerConfig.isInternalHttpsEnabled()) {
            services.add(ServiceInformation.forService(serviceName, serviceType, "https", internalIp.getIp(), internalIp.getHttpsPort()));
        }

        return services;
    }

    public static final Set<ServiceInformation> buildExternalServices(final String serviceName,
                                                                      final String serviceType,
                                                                      final GalaxyHttpServerConfig httpServerConfig,
                                                                      final GalaxyConfig galaxyConfig)
    {
        final Set<ServiceInformation> services = Sets.newHashSet();

        final GalaxyIp externalIp = galaxyConfig.getExternalIp();
        if (httpServerConfig.isExternalHttpEnabled()) {
            services.add(ServiceInformation.forService(serviceName, serviceType, "http", externalIp.getIp(), externalIp.getHttpPort()));
        }
        if (httpServerConfig.isExternalHttpsEnabled()) {
            services.add(ServiceInformation.forService(serviceName, serviceType, "https", externalIp.getIp(), externalIp.getHttpsPort()));
        }

        return services;
    }
}
