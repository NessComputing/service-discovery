package ness.discovery.client;



/**
 * Provides the client side API for service discovery.
 */
public interface DiscoveryClient extends ReadOnlyDiscoveryClient
{
    /**
     * Announces an existing service information. This will be enqueued to the service discovery servers and will be
     * available for service discovery after all servers have picked it up.
     */
    void announce(ServiceInformation serviceInformation);

    /**
     * Unannounce a service information. This uses equals() to determine whether a given service information is present
     * in the client; best is to use the same object that was used for announcing also for unannounce. It is possible that
     * additional service requests are sent in between the unannounce call and all servers and clients having picked up the
     * state.
     */
    void unannounce(ServiceInformation serviceInformation);

    /**
     * Unconditionally unannounce all service information elements registered with this client.
     */
    void unannounceAll();
}
