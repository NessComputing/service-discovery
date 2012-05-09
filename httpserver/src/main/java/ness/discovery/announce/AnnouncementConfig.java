package ness.discovery.announce;

import org.skife.config.Config;
import org.skife.config.Default;
import org.skife.config.DefaultNull;

public abstract class AnnouncementConfig
{
    private final AnnouncementConfig parentConfig;

    public AnnouncementConfig()
    {
        this(null);
    }

    AnnouncementConfig(final AnnouncementConfig parentConfig)
    {
        this.parentConfig = parentConfig;
    }


    /**
     * The service name to announce. Must be set.
     */
    @Config("ness.announce.service-name")
    public String getServiceName()
    {
        return parentConfig != null ? parentConfig.getServiceName() : null;
    }

    /**
     * The service type to annonce. Can be empty.
     */
    @Config("ness.announce.service-type")
    @DefaultNull
    public String getServiceType()
    {
        return parentConfig != null ? parentConfig.getServiceType() : null;
    }

    /**
     * Announce the internal service addresses if configured. When enabled, the server will announce its internal ip and ports.
     *
     * This is enabled by default.
     */
    @Config("ness.announce.internal")
    @Default("true")
    public boolean isAnnounceInternal()
    {
        return parentConfig != null ? parentConfig.isAnnounceInternal() : true;
    }

    /**
     * Announce the external service addresses if configured. When enabled, the server will announce its external ip and ports.
     *
     * This is disabled and should only be enabled for testing/debugging.
     */
    @Config("ness.announce.external")
    @Default("false")
    public boolean isAnnounceExternal()
    {
        return parentConfig != null ? parentConfig.isAnnounceExternal() : false;
    }
}
