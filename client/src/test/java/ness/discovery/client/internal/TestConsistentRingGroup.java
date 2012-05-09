/**
 * 
 */
package ness.discovery.client.internal;

import java.util.List;
import java.util.UUID;

import junit.framework.Assert;
import ness.discovery.client.ServiceInformation;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @author christopher
 *
 */
public class TestConsistentRingGroup {
	private ConsistentRingGroup group;
	
	@Before
	public void setup() {
		List<ServiceInformation> services = Lists.newArrayList();
		services.add(new ServiceInformation("fake", "type1", UUID.randomUUID(), Maps.<String, String>newHashMap()));
		services.add(new ServiceInformation("fake", "type2", UUID.randomUUID(), Maps.<String, String>newHashMap()));
		services.add(new ServiceInformation("fake", null, UUID.randomUUID(), Maps.<String, String>newHashMap()));
		
		group = new ConsistentRingGroup(services);
	}
	
	@Test
	public void testBasic() {
		ConsistentHashRing ring = group.getRing("type1");
		Assert.assertEquals(1, ring.size());
		ServiceInformation service = (ServiceInformation) CollectionUtils.get(ring, 0);
		Assert.assertEquals("type1", service.getServiceType());
	}
	
	@Test
	public void testFallback() {
		ConsistentHashRing ring = group.getRing("type3");
		Assert.assertEquals(1, ring.size());
		ServiceInformation service = (ServiceInformation) CollectionUtils.get(ring, 0);
		Assert.assertEquals(null, service.getServiceType());
	}
}
