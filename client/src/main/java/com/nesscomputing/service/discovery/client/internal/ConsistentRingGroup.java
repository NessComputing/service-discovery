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
package com.nesscomputing.service.discovery.client.internal;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;


import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.nesscomputing.service.discovery.client.ServiceInformation;

/** A group of servers, partitioned into rings by their service type, which provide a particular service.
 *
 * @author christopher
 *
 */
public class ConsistentRingGroup extends AbstractCollection<ConsistentHashRing> {
	private final Map<String, ConsistentHashRing> rings = Maps.newHashMap();
	private final Random rand = new Random();
	private final int totalServers;

	public ConsistentRingGroup(Collection<ServiceInformation> servers) {
		totalServers = servers.size();
		Map<String, List<ServiceInformation>> serverGroups = Maps.newHashMap();
		String serviceName = null;
		//Sort the servers by type
		for (ServiceInformation info: servers) {
			Preconditions.checkArgument(serviceName == null || StringUtils.equals(serviceName, info.getServiceName()),
					"All services must have the same name: " + servers);
			serviceName = info.getServiceName();
			//Hashmaps allow null keys, so null service types should map correctly.
			if (!serverGroups.containsKey(info.getServiceType())) {
				serverGroups.put(info.getServiceType(), new ArrayList<ServiceInformation>());
			}
			serverGroups.get(info.getServiceType()).add(info);
		}

		for (Map.Entry<String, List<ServiceInformation>> entry: serverGroups.entrySet()) {
			rings.put(entry.getKey(), new ConsistentHashRing(entry.getValue()));
		}
	}

	/** Get the server ring for a particular type.
	 *
	 * Notes on running time:
	 * if type != null: O(1)
	 * if type == null and there is at least 1 service with null type: O(1)
	 * otherwise: O(N) where N is the number of types
	 *
	 * @param type
	 * @return
	 */
	public ConsistentHashRing getRing(String type) {
		ConsistentHashRing ring = rings.get(type);
		if (ring != null) {
			return ring;
		}
		if (type == null) {
			//If there's no ring without a type, then any type will do
			//Use weighted random among the types, based on how many servers are serving each type
			int selection = rand.nextInt() % totalServers;
			for (ConsistentHashRing candidateRing: rings.values()) {
				if (selection <= 0) {
					return candidateRing;
				} else {
					selection -= candidateRing.size();
				}
			}
			//It's possible there are no rings
			Preconditions.checkState(totalServers == 0, "It shouldn't be possible to get here, " +
					"unless there are no rings");
			return null;
		} else {
			//If there's no server for this type, then pick one without a type
			return rings.get(null);
		}
	}

	public List<ServiceInformation> getAll() {
		List<ServiceInformation> result = Lists.newArrayList();
		for (Collection<ServiceInformation> info: this) {
			result.addAll(info);
		}
		return result;
	}

	@Override
	public Iterator<ConsistentHashRing> iterator() {
		return rings.values().iterator();
	}

	@Override
	public int size() {
		return rings.size();
	}
}
