package ness.discovery.server.resources;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import ness.discovery.client.ReadOnlyDiscoveryClient;
import ness.discovery.client.ServiceInformation;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

/**
 * Returns the state of the world as a JSON array. Can be limited by serviceName and serviceType.
 */
@Path("/state")
public class StateOfTheWorldResource
{
    private final ReadOnlyDiscoveryClient discoveryClient;

    @Inject
    public StateOfTheWorldResource(final ReadOnlyDiscoveryClient discoveryClient)
    {
        this.discoveryClient = discoveryClient;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getState(@QueryParam("serviceName") final String serviceName,
                             @QueryParam("serviceType") final String serviceType)
    {
        final Collection<ServiceInformation> result;

        if (!StringUtils.isBlank(serviceName)) {
            // serviceType can be null for this call, this returns all services
            result = discoveryClient.findAllServiceInformation(serviceName, serviceType);
        }
        else {
            result = Lists.newArrayList();
            final Map<String, List<ServiceInformation>> serviceMap = discoveryClient.findAllServiceInformation();

            for (List<ServiceInformation> serviceList : serviceMap.values()) {
                result.addAll(serviceList);
            }
        }

        return Response.ok(result).build();
    }
}
