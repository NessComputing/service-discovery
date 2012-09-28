package com.nesscomputing.service.discovery.server.resources;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.nesscomputing.service.discovery.client.DiscoveryClient;
import com.nesscomputing.service.discovery.client.ServiceInformation;

@Path("/announcement")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StaticAnnouncementResource
{
    private final DiscoveryClient discoveryClient;

    @Inject
    StaticAnnouncementResource(DiscoveryClient discoveryClient)
    {
        this.discoveryClient = discoveryClient;
    }

    @POST
    public Response staticAnnounce(Map<String, Object> data)
    {
        final String serviceName = ObjectUtils.toString(data.get("name"), null);
        final String serviceType = ObjectUtils.toString(data.get("type"), null);
        final String serviceScheme = ObjectUtils.toString(data.get("scheme"), null);
        final String serviceAddress = ObjectUtils.toString(data.get("address"), null);

        maybeWhineAbout(serviceName, "name");
        maybeWhineAbout(serviceScheme, "scheme");
        maybeWhineAbout(serviceAddress, "address");

        final int port;

        try {
            port = Integer.parseInt(ObjectUtils.toString(data.get("port"), null));
        } catch (final NumberFormatException e) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(ImmutableMap.of(
                            "detail", "The provided port is not valid: " + e.getMessage(),
                            "errorType", "BAD_ANNOUNCE",
                            "errorSubtype", "BAD_PORT",
                            "field", "port"))
                    .build();
        }

        final ServiceInformation announcement = ServiceInformation.staticAnnouncement(serviceName, serviceType, serviceScheme, serviceAddress, port);

        if (!announcement.isStaticAnnouncement()) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(ImmutableMap.of(
                            "detail", "The provided service announcement must be a static announcement",
                            "errorType", "BAD_ANNOUNCE",
                            "errorSubtype", "NOT_STATIC",
                            "field", "staticAnnouncement"))
                    .build();
        }

        discoveryClient.announce(announcement);

        return Response.status(Status.CREATED).location(URI.create(announcement.getServiceId().toString())).build();
    }

    @GET
    public Map<String, List<ServiceInformation>> getStaticAnnouncements() {
        return ImmutableMap.of("results", findStaticAnnouncements());
    }

    @GET
    @Path("/{id}")
    public ServiceInformation getStaticAnnouncement(@PathParam("id") UUID id) {
        return findStaticAnnouncement(id);
    }

    @DELETE
    @Path("/{id}")
    public Response deleteStaticAnnouncement(@PathParam("id") final UUID serviceId)
    {
        final ServiceInformation victim = findStaticAnnouncement(serviceId);

        if (victim == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        discoveryClient.unannounce(victim);

        return Response.noContent().build();
    }

    private void maybeWhineAbout(String value, String fieldName)
    {
        if (StringUtils.isBlank(value)) {
            throw new WebApplicationException(Response.status(Status.BAD_REQUEST)
                    .entity(ImmutableMap.of(
                            "detail", String.format("The %s field may not be blank", fieldName),
                            "errorType", "BAD_ANNOUNCE",
                            "errorSubtype", "BAD_" + fieldName.toUpperCase(Locale.US),
                            "field", fieldName))
                    .build());
        }
    }

    private List<ServiceInformation> findStaticAnnouncements()
    {
        final Iterable<ServiceInformation> announcements = Iterables.concat(discoveryClient.findAllServiceInformation().values());

        final List<ServiceInformation> results = Lists.newArrayList();

        for (final ServiceInformation si : announcements) {
            if (si.isStaticAnnouncement()) {
                results.add(si);
            }
        }
        return results;
    }

    private ServiceInformation findStaticAnnouncement(final UUID serviceId)
    {
        final Collection<ServiceInformation> victim = Collections2.filter(findStaticAnnouncements(), new Predicate<ServiceInformation>() {
            @Override
            public boolean apply(@Nullable ServiceInformation input)
            {
                return input != null && input.getServiceId().equals(serviceId);
            }
        });
        return Iterables.getOnlyElement(victim, null);
    }
}
