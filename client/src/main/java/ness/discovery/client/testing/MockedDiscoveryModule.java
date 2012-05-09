package ness.discovery.client.testing;

import ness.discovery.client.DiscoveryClient;
import ness.discovery.client.ReadOnlyDiscoveryClient;
import ness.discovery.client.ServiceInformation;

import com.google.inject.AbstractModule;

/**
 * Install a mocked discovery client. Can be used to override an existing module binding to replace
 * service discovery with a mocked version.
 */
public class MockedDiscoveryModule extends AbstractModule
{
    private final String serviceName;
    private final String serviceType;
    private final String serviceScheme;
    private final String serviceAddress;
    private final int servicePort;

    public MockedDiscoveryModule(final String serviceName,
                                      final String serviceType)
    {
        this(serviceName, serviceType, 8080);
    }

    public MockedDiscoveryModule(final String serviceName,
                                      final String serviceType,
                                      final int servicePort)
    {
        this(serviceName, serviceType, "http", "localhost", servicePort);
    }

    public MockedDiscoveryModule(final String serviceName,
                                      final String serviceType,
                                      final String serviceScheme,
                                      final String serviceAddress,
                                      final int servicePort)
    {
        this.serviceName = serviceName;
        this.serviceType = serviceType;
        this.serviceScheme = serviceScheme;
        this.serviceAddress = serviceAddress;
        this.servicePort = servicePort;
    }

    @Override
    public void configure()
    {
        final DiscoveryClient client = MockedDiscoveryClient.builder()
            .addServiceInformation(ServiceInformation.forService(serviceName, serviceType, serviceScheme, serviceAddress, servicePort))
            .build();

        bind(DiscoveryClient.class).toInstance(client);
        bind(ReadOnlyDiscoveryClient.class).toInstance(client);
    }
}
