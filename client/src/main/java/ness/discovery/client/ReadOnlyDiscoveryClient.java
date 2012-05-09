package ness.discovery.client;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * Provides the read-only client side API for service discovery.
 */
public interface ReadOnlyDiscoveryClient
{
    /**
     * Waits until the discovery client has received the first update of the internal service map.
     */
    void waitForWorldChange() throws InterruptedException;

    /**
     * Waits until the discovery client has received the first update of the internal service map.
     */
    boolean waitForWorldChange(final long timeout, final TimeUnit timeUnit) throws InterruptedException;

    /**
     * Returns a service URI for any URI based service. This implies the following information in the
     * discovery service:
     *
     * serviceType
     * serviceAddress
     * servicePort
     *
     * These three pieces are combined into a service URI. No additional check is done to see whether the
     * URI is valid.
     */
    URI findServiceUri(String serviceName, String serviceType, ServiceHint ... hints) throws ServiceNotAvailableException;

    /**
     * Return a service element for a service. The map contains all the pieces of information known for the service.
     */
    ServiceInformation findServiceInformation(String serviceName, String serviceType, ServiceHint ... hints) throws ServiceNotAvailableException;

    /**
     * Return a list of service elements matching the requested service name and type.
     */
    List<ServiceInformation> findAllServiceInformation(String serviceName, String serviceType);

    /**
     * Return a list of service elements for the given service name.
     */
    List<ServiceInformation> findAllServiceInformation(String serviceName);

    /**
     * Return a full view of the service information.
     */
    Map<String, List<ServiceInformation>> findAllServiceInformation();
}
