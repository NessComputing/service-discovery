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


import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.nesscomputing.service.discovery.client.ServiceInformation;
import com.nesscomputing.service.discovery.client.internal.ConsistentHashRing;

/**
 * @author christopher
 *
 */
public class TestConsistentHashRing {
	//Test that removing a server doesn't remap more than ~10% of the keys.
	//This unit test doesn't rely on anything pseudo-random, so it shouldn't fail unexpectedly.
	//If you change the hashing algorithm and this test starts failing,
	//it's probably safe to update it.
	@Test
	public void testConsistency() {
		List<ServiceInformation> servers = Lists.newArrayList();
		for (int i = 0; i < 10; i++) {
			servers.add(new ServiceInformation("blah", "fake", new UUID(0L, (long) i), 
					Maps.<String, String>newHashMap()));
		}
		
		ConsistentHashRing ring = new ConsistentHashRing(servers);
		ConsistentHashRing ring2 = new ConsistentHashRing(servers.subList(1, servers.size()));
		//Make sure that no more than ~10% of the keys have changed assignments
		int differences = 0;
		for (int i = 0; i < 100*10; i++) {
			String key = String.valueOf(i);
			if (!ring.get(key).equals(ring2.get(key))) {
				differences++;
			}
		}
	
		Assert.assertTrue(differences < 150);
	}
}
