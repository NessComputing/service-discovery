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
