package ness.discovery.client.internal;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import ness.discovery.client.DiscoveryClient;
import ness.discovery.client.DiscoveryClientConfig;
import ness.discovery.client.DiscoveryServiceInterceptor;
import ness.discovery.client.ServiceInformation;
import ness.discovery.client.testing.MockedDiscoveryModule;

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
