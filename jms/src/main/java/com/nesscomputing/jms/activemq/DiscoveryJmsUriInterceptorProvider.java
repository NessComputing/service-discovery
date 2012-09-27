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
package com.nesscomputing.jms.activemq;

import java.lang.annotation.Annotation;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import com.nesscomputing.jms.JmsUriInterceptor;
import com.nesscomputing.logging.Log;
import com.nesscomputing.service.discovery.client.ReadOnlyDiscoveryClient;

/**
 * Replace the single format specifier in a srvc:// URI with the unique ID specifying which
 * injector it belongs to.
 */
@Singleton
class DiscoveryJmsUriInterceptorProvider implements Provider<JmsUriInterceptor> {
    private static final Log LOG = Log.findLog();
    private final UUID injectorId = UUID.randomUUID();
    private final Annotation jmsAnnotation;
    private volatile DiscoveryJmsConfig config;

    DiscoveryJmsUriInterceptorProvider(Annotation jmsAnnotation)
    {
        this.jmsAnnotation = jmsAnnotation;
    }

    @Inject
    void injectDiscoveryClient(Injector injector, ReadOnlyDiscoveryClient discoveryClient) {
        config = injector.getInstance(Key.get(DiscoveryJmsConfig.class, jmsAnnotation));

        if (!config.isSrvcTransportEnabled())
        {
            return;
        }

        LOG.debug("Waiting for world change then registering discovery client %s, config %s", injectorId, config);
        // Ensure that we don't register a discovery client until it's had at least one world-change (or give up due to timeout)
        try {
            discoveryClient.waitForWorldChange(config.getDiscoveryTimeout().getMillis(), TimeUnit.MILLISECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ServiceDiscoveryTransportFactory.registerDiscoveryClient(injectorId, discoveryClient, config);
    }

    @Override
    public JmsUriInterceptor get()
    {
        return new DiscoveryJmsUriInterceptor(config, injectorId);
    }
}
