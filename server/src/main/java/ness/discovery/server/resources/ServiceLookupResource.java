package ness.discovery.server.resources;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import ness.discovery.client.ServiceURI;
import ness.discovery.client.ServiceURIConverter;

import com.google.inject.Inject;

/**
 * Converts a given "srvc://" URI to a service-discovery URI.
 */
@Path("/convert")
public class ServiceLookupResource
{
    private final ServiceURIConverter serviceUriConverter;

    @Inject
    public ServiceLookupResource(final ServiceURIConverter serviceUriConverter)
    {
        this.serviceUriConverter = serviceUriConverter;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getURI(@QueryParam("uri") ServiceURI uri)
        throws URISyntaxException, IOException
    {
        if (uri != null) {
            return Response.ok(serviceUriConverter.convertServiceURI(uri).toString()).build();
        }
        else {
            return Response.status(Status.BAD_REQUEST).build();
        }
    }
}
