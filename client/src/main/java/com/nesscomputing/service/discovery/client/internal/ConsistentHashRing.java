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

import java.util.AbstractCollection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.SortedMap;

import javax.annotation.concurrent.Immutable;


import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.nesscomputing.service.discovery.client.ServiceInformation;

/** Data structure that can be used to efficiently find the correct server
 * that serves a particular hash key.
 *
 * Given two ConsistentHashRings that were created from similar lists of servers,
 * "many" keys should result in the same server being chosen.
 *
 * For some background, see: <http://www.last.fm/user/RJ/journal/2007/04/10/rz_libketama_-_a_consistent_hashing_algo_for_memcache_clients>
 *
 * @author christopher
 *
 */
@Immutable
public class ConsistentHashRing extends AbstractCollection<ServiceInformation> {
	private final HashAlgorithm algorithm = HashAlgorithm.FNV1_32_HASH;
	private final SortedMap<Long, ServiceInformation> ring = Maps.newTreeMap();
	private final List<ServiceInformation> servers;

	public ConsistentHashRing(List<ServiceInformation> servers) {
		//Make a copy before sorting the list
		this.servers = Lists.newArrayList(servers);
		//Sort the servers, so that the order in which we insert them into the ring is stable
		Collections.sort(this.servers, new Comparator<ServiceInformation>() {
			@Override
			public int compare(ServiceInformation o1, ServiceInformation o2) {
				return o1.getServiceId().compareTo(o2.getServiceId());
			}
		});

		for (ServiceInformation info: this.servers) {
			//Insert each server at 100 points in the ring, so that load is
			//(more) evenly redistributed if it dies, and a new ring is built from the remaining servers.
			Random rand = new Random(0);
			for (int i = 0; i < 100; i++) {
				//Append a deterministic random sequence
				long hash = algorithm.hash(info.getServiceId().toString() + String.valueOf(rand.nextInt()));
				ring.put(hash, info);
			}
		}
	}

	/** Returns the appropriate server for the given key.
	 *
	 * Running time: O(1)
	 *
	 * @param key
	 * @throws java.util.NoSuchElementException if the ring is empty
	 * @return
	 */
	public ServiceInformation get(String key) {
		ServiceInformation info = null;
		long hash = algorithm.hash(key);
		//Find the first server with a hash key after this one
		final SortedMap<Long, ServiceInformation> tailMap = ring.tailMap(hash);
		//Wrap around to the beginning of the ring, if we went past the last one
		hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
		info = ring.get(hash);
		return info;
	}

	@Override
	public Iterator<ServiceInformation> iterator() {
		return servers.iterator();
	}

	@Override
	public int size() {
		return servers.size();
	}
}
