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
package ness.discovery.client.testing;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import ness.discovery.client.DiscoveryClient;
import ness.discovery.client.ServiceInformation;
import ness.discovery.client.internal.AbstractDiscoveryClient;
import ness.discovery.client.internal.ConsistentRingGroup;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.inject.Singleton;
import com.nesscomputing.logging.Log;

@Singleton
public class MemoryDiscoveryClient extends AbstractDiscoveryClient implements DiscoveryClient {

    private static final Log LOG = Log.findLog();

    public MemoryDiscoveryClient() {
        super(true);

        updateStateOfTheWorld();
    }

    private final Set<ServiceInformation> announcements = new CopyOnWriteArraySet<ServiceInformation>();

    @Override
    public void announce(ServiceInformation serviceInformation) {
        boolean announced = announcements.add(serviceInformation);

        LOG.debug("Announcing new service %s %s", serviceInformation, announced ? "succeeded" : "failed, already announced");

        updateStateOfTheWorld();
    }

    @Override
    public void unannounce(ServiceInformation serviceInformation) {
        boolean unannounced = announcements.remove(serviceInformation);

        LOG.debug("Unannouncing service %s %s", serviceInformation, unannounced ? "succeeded" : "failed, no such announcement");

        updateStateOfTheWorld();
    }

    @Override
    public void unannounceAll() {
        announcements.clear();
        LOG.debug("Cleared all announcements");

        updateStateOfTheWorld();
    }

    private void updateStateOfTheWorld() {

         Map<String, ConsistentRingGroup> newWorldOrder =
                 Maps.transformValues(
                         Multimaps.index(announcements, GET_SERVICE_NAME).asMap(),
                         BUILD_RING_GROUP);

        getStateOfTheWorldHolder().setState(newWorldOrder);

        LOG.trace("Pushed out discovery update, new world order = %s", newWorldOrder);
    }

    private static final Function<ServiceInformation, String> GET_SERVICE_NAME = new Function<ServiceInformation, String>() {
        @Override
        public String apply(ServiceInformation input) {
            if (input == null) {
                return null;
            }
            return input.getServiceName();
        }
    };

    private static final Function<Collection<ServiceInformation>, ConsistentRingGroup> BUILD_RING_GROUP =
        new Function<Collection<ServiceInformation>, ConsistentRingGroup>() {
            @Override
            public ConsistentRingGroup apply(Collection<ServiceInformation> servers) {
                if (servers == null) {
                    return null;
                }
                return new ConsistentRingGroup(servers);
            }
    };
}
