package ness.discovery.client.testing;

import java.util.List;
import java.util.Map;
import java.util.Set;

import ness.discovery.client.DiscoveryClient;
import ness.discovery.client.ServiceInformation;
import ness.discovery.client.internal.ConsistentRingGroup;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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
