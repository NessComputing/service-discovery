package ness.discovery.client.testing;

import java.util.List;
import java.util.Map;

import ness.discovery.client.ReadOnlyDiscoveryClient;
import ness.discovery.client.ServiceInformation;
import ness.discovery.client.internal.AbstractDiscoveryClient;
import ness.discovery.client.internal.ConsistentRingGroup;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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
