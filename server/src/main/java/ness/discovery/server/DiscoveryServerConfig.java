package ness.discovery.server;

import org.skife.config.Config;
import org.skife.config.Default;

public abstract class DiscoveryServerConfig
{
    /**
     * Root of the service discovery tree. Defaults to "/ness/srvc".
     */
    @Config("ness.discovery.root")
    @Default("/ness/srvc")
    public String getRoot()
    {
        return "/ness/srvc";
    }
}
