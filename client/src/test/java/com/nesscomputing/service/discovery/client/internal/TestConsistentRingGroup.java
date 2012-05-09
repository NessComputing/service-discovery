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
/**
 * 
 */
package com.nesscomputing.service.discovery.client.internal;

import java.util.List;
import java.util.UUID;

import junit.framework.Assert;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.nesscomputing.service.discovery.client.ServiceInformation;
import com.nesscomputing.service.discovery.client.internal.ConsistentHashRing;
import com.nesscomputing.service.discovery.client.internal.ConsistentRingGroup;

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
