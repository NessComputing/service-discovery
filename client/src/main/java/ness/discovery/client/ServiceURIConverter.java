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
import java.net.URISyntaxException;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * Convert an URI starting with "srvc://" into a regular http/https URI using service discovery.
 */
@Singleton
public class ServiceURIConverter
{
    private final ReadOnlyDiscoveryClient discoveryClient;

    private final DiscoveryClientConfig discoveryClientConfig;

    @Inject
    ServiceURIConverter(final ReadOnlyDiscoveryClient discoveryClient,
                        final DiscoveryClientConfig discoveryClientConfig)
    {
        this.discoveryClient = discoveryClient;
        this.discoveryClientConfig = discoveryClientConfig;
    }

    public URI convertServiceURI(@Nonnull final ServiceURI serviceUri, ServiceHint ... hints)
        throws URISyntaxException, ServiceNotAvailableException
    {
        Preconditions.checkArgument(serviceUri != null, "serviceURI can not be null!");

        if (!discoveryClientConfig.isEnabled()) {
            throw new ServiceNotAvailableException("Service discovery is disabled!");
        }

        final ServiceInformation serviceInfo = discoveryClient.findServiceInformation(serviceUri.getServiceName(), serviceUri.getServiceType(), hints);

        final String scheme = serviceInfo.getProperty(ServiceInformation.PROP_SERVICE_SCHEME);
        final String address = serviceInfo.getProperty(ServiceInformation.PROP_SERVICE_ADDRESS);
        if (StringUtils.isEmpty(scheme) || StringUtils.isEmpty(address)) {
            throw new ServiceNotAvailableException("Service Information %s is incomplete!", serviceInfo);
        }

        final StringBuilder sb = new StringBuilder(scheme).append("://");
        sb.append(address);

        final String portStr = serviceInfo.getProperty(ServiceInformation.PROP_SERVICE_PORT);

        int port = -1;
        try {
            port = Integer.parseInt(portStr);
        }
        catch (NumberFormatException nfe) {
            throw new ServiceNotAvailableException("Bad port (%s) in Service Information %s is incomplete!", portStr, serviceInfo);
        }

        if ("http".equals(scheme) && port == 80) {
            port = -1;
        }
        else if ("https".equals(scheme) && port == 443) {
            port = -1;
        }

        if (port != -1) {
            sb.append(":").append(port);
        }

        final String path = serviceUri.getPath();
        if (!StringUtils.isBlank(path)) {
            sb.append(path);
        }
        final String query = serviceUri.getQuery();
        if (!StringUtils.isBlank(query)) {
            sb.append("?").append(query);
        }
        final String fragment = serviceUri.getFragment();
        if (!StringUtils.isBlank(fragment)) {
            sb.append("#").append(fragment);
        }

        // Any other c'tor does double-escape of the raw elements in path, query and fragment.
        return new URI(sb.toString());
    }
}
