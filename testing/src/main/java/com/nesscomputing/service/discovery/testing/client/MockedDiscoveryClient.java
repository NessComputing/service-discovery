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
package com.nesscomputing.service.discovery.testing.client;

import java.util.List;
import java.util.Map;
import java.util.Set;


import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.nesscomputing.service.discovery.client.DiscoveryClient;
import com.nesscomputing.service.discovery.client.ServiceInformation;
import com.nesscomputing.service.discovery.client.internal.ConsistentRingGroup;

/**
 * Mocked read/write implementation of the Discovery client. Useful for testing.
 */
public class MockedDiscoveryClient extends MockedReadOnlyDiscoveryClient implements DiscoveryClient
{
    public MockedDiscoveryClient(final Map<String, ConsistentRingGroup> newWorldOrder)
    {
        super(newWorldOrder);
    }

    @Override
    public void unannounceAll()
    {
        final Map<String, ConsistentRingGroup> worldOrder = getStateOfTheWorldHolder().getState();
        worldOrder.clear();
    }

    @Override
    public void announce(final ServiceInformation serviceInformation)
    {
        Preconditions.checkArgument(serviceInformation != null, "serviceInformation must not be null!");

        final Map<String, ConsistentRingGroup> worldOrder = getStateOfTheWorldHolder().getState();
        final String serviceName = serviceInformation.getServiceName();
        List<ServiceInformation> services = Lists.newArrayList();
        ConsistentRingGroup currentGroup = worldOrder.get(serviceName);
        if (currentGroup != null) {
            services.addAll(currentGroup.getAll());
        }
        if (!services.contains(serviceInformation)) {
            services.add(serviceInformation);
            worldOrder.put(serviceName, new ConsistentRingGroup(services));
        }
    }

    @Override
    public void unannounce(final ServiceInformation serviceInformation)
    {
        Preconditions.checkArgument(serviceInformation != null, "serviceInformation must not be null!");

        final Map<String, ConsistentRingGroup> worldOrder = getStateOfTheWorldHolder().getState();
        ConsistentRingGroup group = worldOrder.get(serviceInformation.getServiceName());
        if (group == null) {
        	return;
        }
        Set<ServiceInformation> services = Sets.newHashSet(group.getAll());
        services.remove(serviceInformation);
        worldOrder.put(serviceInformation.getServiceName(), new ConsistentRingGroup(services));
    }

    public static Builder<? extends DiscoveryClient> builder()
    {
        return new Builder<DiscoveryClient>(MockedDiscoveryClient.class);
    }

    public static class Builder<Type> extends MockedReadOnlyDiscoveryClient.Builder<Type>
    {
        <T> Builder(final Class<? extends Type> clazz)
        {
            super(clazz);
        }

        @Override
        public Type build()
        {
            return clazz.cast(new MockedDiscoveryClient(newWorldOrder));
        }
    }
}
