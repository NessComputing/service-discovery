package com.nesscomputing.jms.activemq;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.nesscomputing.config.Config;
import com.nesscomputing.config.ConfigProvider;
import com.nesscomputing.jms.JmsUriInterceptor;

/**
 * Enables service discovery transport for JMS
 *
 * @see ServiceDiscoveryTransportFactory Configuration specifics
 */
public class DiscoveryJmsModule extends AbstractModule
{
    private final Config config;

    public DiscoveryJmsModule(Config config)
    {
        this.config = config;
    }

    @Override
    protected void configure()
    {
        if (config.getBean(DiscoveryJmsConfig.class).isSrvcTransportEnabled()) {
            Multibinder.newSetBinder(binder(), JmsUriInterceptor.class).addBinding().to(
                    DiscoveryJmsUriInterceptor.class)
                    .in(Scopes.SINGLETON);

            bind (DiscoveryJmsConfig.class).toProvider(
                    ConfigProvider.of(null, DiscoveryJmsConfig.class))
                    .in(Scopes.SINGLETON);
        }
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((config == null) ? 0 : config.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final DiscoveryJmsModule other = (DiscoveryJmsModule) obj;
        if (config == null) {
            if (other.config != null)
                return false;
        } else if (!config.equals(other.config))
            return false;
        return true;
    }
}
