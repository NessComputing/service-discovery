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
package com.nesscomputing.jms.activemq;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import net.sf.cglib.proxy.Enhancer;

import org.apache.activemq.transport.Transport;
import org.apache.activemq.transport.TransportFactory;
import org.apache.activemq.transport.TransportServer;
import org.apache.activemq.util.URISupport;
import org.apache.activemq.wireformat.WireFormat;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import com.nesscomputing.logging.Log;
import com.nesscomputing.service.discovery.client.ReadOnlyDiscoveryClient;
import com.nesscomputing.service.discovery.client.ServiceInformation;

/**
 * Handle srvc:// URIs by doing discovery and turning them into a failover transport.
 *
 * For example if you connect to srvc://activemq?discoveryId=x&serviceType=y this transport will look
 * up the injector whose discoveryId is "x".  It will then proceed to discover all services with
 * name "activemq" and type "y".  They all must advertise URIs, which will then get built into a
 * <code>failover:(x,y)</code> transport.
 *
 * This code uses a static map to coordinate DiscoveryClients since the ActiveMQ TransportFactory
 * abstraction is static in nature.  A number of possibilities were examined but it seems impossible
 * to register a transport in a way that is truly IoC-friendly.
 */
public class ServiceDiscoveryTransportFactory extends TransportFactory {

    private static final Log LOG = Log.findLog();

    private static final ConcurrentMap<UUID, ReadOnlyDiscoveryClient> DISCO_CLIENTS = Maps.newConcurrentMap();
    private static final ConcurrentMap<UUID, DiscoveryJmsConfig> CONFIGS = Maps.newConcurrentMap();

    /**
     * Register a discoveryClient under a unique id.  This works around the TransportFactory's inherent staticness
     * so that we may use the correct discovery client even in the presence of multiple injectors in the same JVM.
     */
    static void registerDiscoveryClient(UUID injectorId, ReadOnlyDiscoveryClient discoveryClient, DiscoveryJmsConfig config) {
        DISCO_CLIENTS.put(injectorId, discoveryClient);
        CONFIGS.put(injectorId, config);
        LOG.info("Registered discovery client %s as %s", injectorId, discoveryClient);
    }

    @Override
    public TransportServer doBind(URI location) throws IOException {
        throw new IOException("The srvc transport is inappropriate for specifying a server location: " + location);
    }

    @Override
    protected Transport createTransport(URI location, WireFormat wf)
    throws MalformedURLException, UnknownHostException, IOException {

        final Map<String, String> params = findParameters(location);
        final List<ServiceInformation> services = getServiceInformations(location, params);
        return buildTransport(params, services);

    }

    /**
     * Extract the query string from a connection URI into a Map
     */
    @SuppressWarnings("PMD.PreserveStackTrace")
    private Map<String, String> findParameters(URI location) throws MalformedURLException {
        final Map<String, String> params;
        try {
            params = URISupport.parseParameters(location);
        } catch (final URISyntaxException e) {
            throw new MalformedURLException(e.getMessage());
        }
        return params;
    }

    /**
     * Find and validate the discoveryId given in a connection string
     */
    private UUID getDiscoveryId(final Map<String, String> params) throws IOException {
        final String discoveryId = params.get("discoveryId");

        if (discoveryId == null) {
            throw new IOException("srvc transport did not get a discoveryId parameter.  Refusing to create.");
        }
        return UUID.fromString(discoveryId);
    }

    /**
     * Locate the appropriate DiscoveryClient for a given discoveryId
     */
    private ReadOnlyDiscoveryClient getDiscoveryClient(Map<String, String> params) throws IOException {
        final UUID discoveryId = getDiscoveryId(params);

        final ReadOnlyDiscoveryClient discoveryClient = DISCO_CLIENTS.get(discoveryId);

        if (discoveryClient == null) {
            throw new IOException("No discovery client registered for id " + discoveryId);
        }
        return discoveryClient;
    }

    private DiscoveryJmsConfig getConfig(Map<String, String> params) throws IOException {
        return CONFIGS.get(getDiscoveryId(params));
    }

    /**
     * Find and return applicable services for a given connection
     */
    private List<ServiceInformation> getServiceInformations(URI location, final Map<String, String> params)
            throws IOException {
        final ReadOnlyDiscoveryClient discoveryClient = getDiscoveryClient(params);

        final String serviceType = params.get("serviceType");

        final List<ServiceInformation> services;

        if (serviceType != null) {
            services = discoveryClient.findAllServiceInformation(location.getHost(), serviceType);
        } else {
            services = discoveryClient.findAllServiceInformation(location.getHost());
        }
        return services;
    }

    /**
     * From a list of ServiceInformation, build a failover transport that balances between the brokers.
     */
    private Transport buildTransport(Map<String, String> params, final List<ServiceInformation> services) throws IOException {
        final String configPostfix = getConfig(params).getServiceConfigurationPostfix();
        final StringBuilder uriBuilder = new StringBuilder();
        uriBuilder.append("failover:(");
        uriBuilder.append(Joiner.on(',').join(Collections2.transform(services, SERVICE_TO_URI)));
        uriBuilder.append(')');
        if (!StringUtils.isBlank(configPostfix)) {
            uriBuilder.append('?');
            uriBuilder.append(configPostfix);
        }
        try {
            final URI uri = URI.create(uriBuilder.toString());
            LOG.debug("Service discovery transport discovered %s", uri);
            return interceptPropertySetters(TransportFactory.compositeConnect(uri));
        } catch (final Exception e) {
            Throwables.propagateIfPossible(e, IOException.class);
            throw new IOException("Could not create failover transport", e);
        }
    }

    /**
     * ActiveMQ expects to reflectively call setX for every property x specified in the connection string.
     * Since we are actually constructing a failover transport, these properties are obviously not expected
     * and ActiveMQ complains that we are specifying invalid parameters.  So create a CGLIB proxy that
     * intercepts and ignores appropriate setter calls.
     */
    private Transport interceptPropertySetters(Transport transport) {
        final Enhancer e = new Enhancer();
        e.setInterfaces(new Class<?>[] {Transport.class, ServiceTransportBeanSetters.class});
        final TransportDelegationFilter filter = new TransportDelegationFilter(transport, ServiceTransportBeanSetters.class);
        e.setCallbackFilter(filter);
        e.setCallbacks(filter.getCallbacks());
        return (Transport) e.create();
    }

    private static final Function<ServiceInformation, String> SERVICE_TO_URI = new Function<ServiceInformation, String>() {
        @Override
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
        public String apply(ServiceInformation input) {
            final String uri = input.getProperty("uri");
            Preconditions.checkArgument(!StringUtils.isBlank(uri), "service did not advertise a connect uri");
            return uri;
        }
    };
}
