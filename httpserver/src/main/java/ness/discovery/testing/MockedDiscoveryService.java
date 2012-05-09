package ness.discovery.testing;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.util.Modules;

import ness.discovery.client.DiscoveryClient;
import ness.discovery.client.DiscoveryClientModule;
import ness.discovery.client.ReadOnlyDiscoveryClient;
import ness.discovery.client.testing.MemoryDiscoveryClient;
import ness.testing.MockedService;

public class MockedDiscoveryService implements MockedService {

    private final DiscoveryClient mockedClient = new MemoryDiscoveryClient();

    @Override
    public Module getServiceModule(String serviceName) {
        return Modules.combine(getTestCaseModule(), new HttpServerAnnouncerModule());
    }

    @Override
    public Module getTestCaseModule() {

        return Modules.override(new DiscoveryClientModule()).with(new AbstractModule() {
            @Override
            protected void configure() {
                bind (DiscoveryClient.class).toInstance(mockedClient);
                bind (ReadOnlyDiscoveryClient.class).toInstance(mockedClient);
            }
        });
    }

    @Override
    public Map<String, String> getServiceConfigTweaks(String serviceName) {
        return getTestCaseConfigTweaks();
    }

    @Override
    public Map<String, String> getTestCaseConfigTweaks() {
        return ImmutableMap.of("ness.discovery.enabled", "true");
    }
}
