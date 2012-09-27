package com.nesscomputing.jms.activemq;

import java.lang.annotation.Annotation;
import java.util.Collections;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.nesscomputing.config.Config;
import com.nesscomputing.config.ConfigProvider;
import com.nesscomputing.jms.JmsModule;
import com.nesscomputing.jms.JmsUriInterceptor;

/**
 * Service discovery enabled variant of {@link JmsModule}.
 *
 * This module will install a <code>JmsModule</code> for you, and
 * enables that module to use connections that have a <code>srvc:</code> URI
 * scheme.
 *
 *  @see ServiceDiscoveryTransportFactory Configuration specifics
 */
public class DiscoveryJmsModule extends AbstractModule
{
    private final String jmsConnectionBindingName;
    private final Config config;

    public DiscoveryJmsModule(Config config, String jmsConnectionBindingName)
    {
        this.config = config;
        this.jmsConnectionBindingName = jmsConnectionBindingName;
    }

    @Override
    protected void configure()
    {
        final Annotation bindingAnnotation = Names.named(jmsConnectionBindingName);

        if (config.getBean(DiscoveryJmsConfig.class).isSrvcTransportEnabled()) {
            Multibinder.newSetBinder(binder(), JmsUriInterceptor.class, bindingAnnotation).addBinding().toProvider(
                    new DiscoveryJmsUriInterceptorProvider(bindingAnnotation))
                    .in(Scopes.SINGLETON);

            bind (DiscoveryJmsConfig.class).annotatedWith(bindingAnnotation).toProvider(
                    ConfigProvider.of(null, DiscoveryJmsConfig.class, Collections.singletonMap("name", jmsConnectionBindingName)))
                    .in(Scopes.SINGLETON);
        }

        install (new JmsModule(config, jmsConnectionBindingName));
    }
}
