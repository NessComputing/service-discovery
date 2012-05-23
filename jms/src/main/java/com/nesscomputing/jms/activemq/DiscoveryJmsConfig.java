package com.nesscomputing.jms.activemq;

import java.util.concurrent.TimeUnit;

import org.skife.config.Config;
import org.skife.config.Default;
import org.skife.config.TimeSpan;

abstract class DiscoveryJmsConfig
{
    /**
     * If true, enables a ConnectionFactory interceptor that adds srvc: URI support
     * to ActiveMQ.  Should only need to be disabled in the rare case that you have
     * a literal %s embedded in the connect string, but don't actually want it to try to discover a broker.
     */
    @Config("ness.jms.srvc.enabled")
    @Default("true")
    public boolean isSrvcTransportEnabled()
    {
        return true;
    }

    /**
     * A literal string to append to the failover: URI that the service discovery transport creates.
     * Do not include the ? for the query string.
     */
    @Config("ness.jms.srvc.configuration")
    @Default("")
    public String getServiceConfigurationPostfix()
    {
        return "";
    }

    /**
     * The amount of time JMS will wait for discovery to synchronize to the central directory
     */
    @Config("ness.jms.srvc.discovery-timeout")
    @Default("2s")
    public TimeSpan getDiscoveryTimeout()
    {
        return new TimeSpan(2, TimeUnit.SECONDS);
    }
}
