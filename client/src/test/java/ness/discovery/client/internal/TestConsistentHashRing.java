/**
 * 
 */
package ness.discovery.client.internal;


import java.util.List;
import java.util.UUID;

import ness.discovery.client.ServiceInformation;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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
