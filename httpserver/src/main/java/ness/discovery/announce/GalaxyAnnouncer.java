package ness.discovery.announce;

import java.util.Set;

import ness.discovery.client.DiscoveryClient;
import ness.discovery.client.ServiceAnnouncer;
import ness.discovery.client.ServiceInformation;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.nesscomputing.galaxy.GalaxyConfig;
import com.nesscomputing.galaxy.GalaxyIp;
import com.nesscomputing.httpserver.HttpServerConfig;
import com.nesscomputing.logging.Log;

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
                    final HttpServerConfig httpServerConfig,
                    final GalaxyConfig galaxyConfig)
    {
        super(discoveryClient);
        setServices(GalaxyAnnouncer.buildServices(announcementConfig, httpServerConfig, galaxyConfig));
    }

    public static final Set<ServiceInformation> buildServices(final AnnouncementConfig announcementConfig,
                                                              final HttpServerConfig httpServerConfig,
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
                                                                      final HttpServerConfig httpServerConfig,
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
                                                                      final HttpServerConfig httpServerConfig,
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
