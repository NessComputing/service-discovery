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
package com.nesscomputing.service.discovery.server.resources;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;


import com.google.inject.Inject;
import com.nesscomputing.service.discovery.client.ServiceURI;
import com.nesscomputing.service.discovery.client.ServiceURIConverter;

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
