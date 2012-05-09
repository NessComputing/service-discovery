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


import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.nesscomputing.service.discovery.client.ReadOnlyDiscoveryClient;
import com.nesscomputing.service.discovery.client.ServiceInformation;
import com.nesscomputing.service.discovery.client.internal.AbstractDiscoveryClient;
import com.nesscomputing.service.discovery.client.internal.ConsistentRingGroup;

/**
 * Mocked read-only implementation of the Discovery client. Useful for testing.
 */
public class MockedReadOnlyDiscoveryClient extends AbstractDiscoveryClient implements ReadOnlyDiscoveryClient
{
    public MockedReadOnlyDiscoveryClient(final Map<String, ConsistentRingGroup> newWorldOrder)
    {
        super(true);
        getStateOfTheWorldHolder().setState(newWorldOrder);
    }

    public static Builder<? extends ReadOnlyDiscoveryClient> builder()
    {
        return new Builder<ReadOnlyDiscoveryClient>(MockedReadOnlyDiscoveryClient.class);
    }

    public static class Builder<Type>
    {
        protected final Map<String, ConsistentRingGroup> newWorldOrder = Maps.newHashMap();
        protected final Class<? extends Type> clazz;

        <T> Builder(final Class<? extends Type> clazz)
        {
            this.clazz = clazz;
        }

        public Builder<Type> addServiceInformation(final ServiceInformation serviceInformation)
        {
            Preconditions.checkArgument(serviceInformation != null, "serviceInformation must not be null");
            final String serviceName = serviceInformation.getServiceName();
            List<ServiceInformation> serviceInformations = Lists.newArrayList();
            ConsistentRingGroup currentGroup = newWorldOrder.get(serviceName);
			if (currentGroup != null) {
            	serviceInformations.addAll(currentGroup.getAll());
            }
			serviceInformations.add(serviceInformation);
			newWorldOrder.put(serviceName, new ConsistentRingGroup(serviceInformations));
            return this;
        }

        public Type build()
        {
            return clazz.cast(new MockedReadOnlyDiscoveryClient(newWorldOrder));
        }
    }
}
