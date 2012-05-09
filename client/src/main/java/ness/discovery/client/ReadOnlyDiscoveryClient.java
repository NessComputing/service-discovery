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
