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

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;


import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import com.nesscomputing.config.ConfigModule;
import com.nesscomputing.httpclient.HttpClient;
import com.nesscomputing.httpclient.HttpClientRequest;
import com.nesscomputing.httpclient.HttpClientResponse;
import com.nesscomputing.httpclient.HttpClientResponseHandler;
import com.nesscomputing.httpclient.guice.HttpClientModule;
import com.nesscomputing.lifecycle.guice.LifecycleModule;
import com.nesscomputing.service.discovery.client.DiscoveryClient;
import com.nesscomputing.service.discovery.client.DiscoveryClientConfig;
import com.nesscomputing.service.discovery.client.DiscoveryServiceInterceptor;
import com.nesscomputing.service.discovery.client.ServiceInformation;
import com.nesscomputing.service.discovery.client.testing.MockedDiscoveryModule;

/**
 * @author christopher
 *
 */
public class TestDiscoveryInterceptor {
	@Inject
	private DiscoveryServiceInterceptor interceptor;
	@Inject
	@Named("fake")
	private HttpClient client;

	@Before
	public void setup() {
		Injector injector = Guice.createInjector(new MockedDiscoveryModule("fake", "fake", "http", "localhost", 12345),
		                                         ConfigModule.forTesting(),
				new HttpClientModule("fake"), new LifecycleModule(), new AbstractModule() {
					@Override
					protected void configure() {
						//Discovery needs to be enabled for the interceptor to do anything
						bind(DiscoveryClientConfig.class).toInstance(new DiscoveryClientConfig() {
							@Override
							public boolean isEnabled() {
								return true;
							}
						});
					}
				});
		DiscoveryClient dc = injector.getInstance(DiscoveryClient.class);
		dc.announce(ServiceInformation.forService("fake", "fake", "http", "localhost", 123456));
		injector.injectMembers(this);
	}

	@Test
	public void testConsistentHashDistribution() throws IOException {
		Set<URI> uris = Sets.newHashSet(mapHashes().values());
		Assert.assertEquals(2, uris.size());
	}

	@Test
	public void testConsistentHashing() throws IOException {
		Assert.assertEquals(mapHashes(), mapHashes());
	}

	private Map<Integer, URI> mapHashes() throws IOException {
		Map<Integer, URI> mapping = Maps.newHashMap();
		for (int i = 0; i < 100; i++) {
			HttpClientRequest.Builder<Void> builder = client.get("srvc://fake:fake/", new HttpClientResponseHandler<Void>() {
				@Override
				public Void handle(HttpClientResponse response)
						throws IOException {
					return null;
				}
			});
			HttpClientRequest<Void> request = builder.addHeader("X-Ness-SDHint-ConsistentHash", String.valueOf(i)).request();
			request = interceptor.onRequestSubmitted(request);
			mapping.put(i, request.getUri());
		}
		return mapping;
	}
}
