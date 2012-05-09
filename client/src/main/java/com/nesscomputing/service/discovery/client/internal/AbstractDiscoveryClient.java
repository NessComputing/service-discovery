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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;


import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Singleton;
import com.nesscomputing.service.discovery.client.ReadOnlyDiscoveryClient;
import com.nesscomputing.service.discovery.client.ServiceHint;
import com.nesscomputing.service.discovery.client.ServiceInformation;
import com.nesscomputing.service.discovery.client.ServiceNotAvailableException;

/**
 * Base class for service discovery. Supplies all the meat to build a read-only
 * client, but no actual code that would insert service information into the
 * state of the world.
 */
@Singleton
public abstract class AbstractDiscoveryClient implements
		ReadOnlyDiscoveryClient {
	private final StateOfTheWorldHolder stateHolder;

	private final Random random = new Random();

	protected AbstractDiscoveryClient(final boolean enabled) {
		// Only wait for the first service update to happen if service discovery
		// is actually enabled.
		this.stateHolder = new StateOfTheWorldHolder(enabled);
	}

	@Override
	public void waitForWorldChange() throws InterruptedException {
		stateHolder.waitForWorldChange();
	}

	@Override
	public boolean waitForWorldChange(final long timeout,
			final TimeUnit timeUnit) throws InterruptedException {
		return stateHolder.waitForWorldChange(timeout, timeUnit);
	}

	protected StateOfTheWorldHolder getStateOfTheWorldHolder() {
		return stateHolder;
	}

	@Override
	public URI findServiceUri(final String serviceName,
			final String serviceType, final ServiceHint... hints)
			throws ServiceNotAvailableException {
		final ServiceInformation service = findServiceInformation(serviceName,
				serviceType, hints);

		final String locatedScheme = service
				.getProperty(ServiceInformation.PROP_SERVICE_SCHEME);
		final String locatedAddress = service
				.getProperty(ServiceInformation.PROP_SERVICE_ADDRESS);
		final String locatedPort = service
				.getProperty(ServiceInformation.PROP_SERVICE_PORT);

		if (StringUtils.isEmpty(locatedScheme)
				|| StringUtils.isEmpty(locatedAddress)
				|| StringUtils.isEmpty(locatedPort)) {
			throw new ServiceNotAvailableException(
					"Service %s/%s exists but misses address information (%s/%s)",
					serviceName, serviceType, locatedAddress, locatedPort);
		}

		try {
			return new URI(locatedScheme, null, locatedAddress,
					Integer.parseInt(locatedPort), "", null, null);
		} catch (URISyntaxException use) {
			throw new ServiceNotAvailableException(
					"Could not create URI from '%s'!", service);
		}
	}

	@Override
	public ServiceInformation findServiceInformation(final String serviceName,
			final String serviceType, final ServiceHint... hints)
			throws ServiceNotAvailableException {
		final ConsistentHashRing services = findRing(serviceName, serviceType);

		if (CollectionUtils.isEmpty(services)) {
			throw new ServiceNotAvailableException("No %s/%s service found",
					serviceName, serviceType);
		}

		final ServiceInformation service = selectHintedService(services, hints);
		if (service == null) {
			throw new ServiceNotAvailableException("No %s/%s service found",
					serviceName, serviceType);
		}
		return service;
	}

	@Override
	public List<ServiceInformation> findAllServiceInformation(
			final String serviceName, final String serviceType) {
		ConsistentHashRing services = findRing(serviceName, serviceType);
		if (CollectionUtils.isEmpty(services)) {
			return Collections.emptyList();
		}

		return Lists.newArrayList(services);
	}

	private ConsistentHashRing findRing(final String serviceName,
			final String serviceType) {
		final Map<String, ConsistentRingGroup> current = stateHolder.getState();
		ConsistentRingGroup group = current.get(serviceName);
		if (CollectionUtils.isEmpty(group)) {
			return null;
		}
		// getRing will fall back to another type (if appropriate), if the
		// requested type can't be found
		return group.getRing(serviceType);
	}

	@Override
	public List<ServiceInformation> findAllServiceInformation(
			final String serviceName) {
		List<ServiceInformation> result = Lists.newArrayList();
		ConsistentRingGroup group = stateHolder.getState().get(serviceName);
		if (CollectionUtils.isEmpty(group)) {
			return Collections.emptyList();
		}
		for (ConsistentHashRing ring : group) {
			result.addAll(ring);
		}
		return result;
	}

	@Override
	public Map<String, List<ServiceInformation>> findAllServiceInformation() {
		final Map<String, ConsistentRingGroup> current = stateHolder.getState();

		// Do a deep copy to make sure no one can hold a reference to the full
		// map or the lists.
		final Map<String, List<ServiceInformation>> result = Maps.newHashMap();
		for (Map.Entry<String, ConsistentRingGroup> entry : current.entrySet()) {
			final List<ServiceInformation> serviceList = Lists.newArrayList();
			for (ConsistentHashRing ring : entry.getValue()) {
				// The service information elements are immutable, so you can
				// hold a reference to them,
				// it does not matter. Please don't. :-)
				serviceList.addAll(ring);
			}
			result.put(entry.getKey(), serviceList);
		}
		return result;
	}

	private ServiceInformation selectHintedService(
			final ConsistentHashRing ring, final ServiceHint... hints) {
		if (CollectionUtils.isEmpty(ring)) {
			return null;
		}

		String hashKey = null;
		for (ServiceHint hint : hints) {
			if (ServiceHint.CONSISTENTHASH_HINT.equals(hint.getName())) {
				hashKey = hint.getValue();
				break;
			}
		}

		if (hashKey != null) {
			return ring.get(hashKey);
		} else {
			return ring.get(String.valueOf(random.nextInt()));
		}
	}
}
