package com.nesscomputing.jms.activemq;

import java.lang.annotation.Annotation;
import java.util.Collections;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.nesscomputing.config.Config;
import com.nesscomputing.config.ConfigProvider;
import com.nesscomputing.jms.JmsModule;
import com.nesscomputing.jms.JmsUriInterceptor;

/**
 * Service discovery enabled varaint of {@link JmsModule}.
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
        Annotation bindingAnnotation = Names.named(jmsConnectionBindingName);

        Multibinder.newSetBinder(binder(), JmsUriInterceptor.class, bindingAnnotation).addBinding().toInstance(
                new DiscoveryJmsUriInterceptor(bindingAnnotation));
        bind (DiscoveryJmsConfig.class).annotatedWith(bindingAnnotation)
            .toProvider(ConfigProvider.of(null, DiscoveryJmsConfig.class, Collections.singletonMap("name", jmsConnectionBindingName)));

        install (new JmsModule(config, jmsConnectionBindingName));
    }
}
