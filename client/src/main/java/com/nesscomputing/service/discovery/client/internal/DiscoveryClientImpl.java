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
package com.nesscomputing.service.discovery.client.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.nesscomputing.lifecycle.Lifecycle;
import com.nesscomputing.lifecycle.LifecycleListener;
import com.nesscomputing.lifecycle.LifecycleStage;
import com.nesscomputing.logging.Log;
import com.nesscomputing.service.discovery.client.DiscoveryClient;
import com.nesscomputing.service.discovery.client.DiscoveryClientConfig;
import com.nesscomputing.service.discovery.client.DiscoveryClientModule;
import com.nesscomputing.service.discovery.client.ServiceInformation;

/**
 * Read/Write service discovery client. Supports service lookup and announcing of local services using Zookeeper.
 */
@Singleton
public class DiscoveryClientImpl extends ReadOnlyDiscoveryClientImpl implements DiscoveryClient
 {
     private static final Log LOG = Log.findLog();

     private final ServiceDiscoveryAnnouncer announcer;

     @Inject
     public DiscoveryClientImpl(@Named(DiscoveryClientModule.ZOOKEEPER_CONNECT_NAME) final String connectString,
                                final DiscoveryClientConfig discoveryConfig,
                                final ObjectMapper objectMapper)
     {
         super(connectString, discoveryConfig, objectMapper);

         announcer = new ServiceDiscoveryAnnouncer(discoveryConfig, objectMapper);

         if (discoveryConfig.isAnnounceEnabled()) {
             serviceDiscoveryVisitors.add(announcer);
         }
         else {
             LOG.info("Service announcement is administratively disabled!");
         }
     }

     @Inject(optional=true)
     @Override
     public void injectLifecycle(final Lifecycle lifecycle)
     {
         super.injectLifecycle(lifecycle);

         lifecycle.addListener(LifecycleStage.UNANNOUNCE_STAGE, new LifecycleListener() {
             @Override
             public void onStage(final LifecycleStage stage) {
                 DiscoveryClientImpl.this.unannounceAll();
             }
         });
     }

     @Override
     public void unannounceAll()
     {
         announcer.unannounceAll();
     }

     @Override
     public void announce(final ServiceInformation serviceInformation)
     {
         LOG.debug("Announcing %s", serviceInformation);
         announcer.announce(serviceInformation);
     }

     @Override
     public void unannounce(final ServiceInformation serviceInformation)
     {
         LOG.debug("Unannouncing %s", serviceInformation);
         announcer.unannounce(serviceInformation);
     }
}
