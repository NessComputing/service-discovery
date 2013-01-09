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
package com.nesscomputing.service.discovery.client;

import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Groups a single service information.
 *
 * Minimum information is:
 *  - serviceName  (e.g. "user-service")
 *  - serviceType  (e.g. "default" or "vip")
 *  - serviceId    (UUID)
 *
 * Additionally, there is a grab bag of information, which may or may not contain things like IP addresses, ports that then can be used
 * by code built on top of the service information.
 *
 */
public class ServiceInformation
{
    public static final String PROP_SERVICE_SCHEME = "serviceScheme";
    public static final String PROP_SERVICE_ADDRESS = "serviceAddress";
    public static final String PROP_SERVICE_PORT = "servicePort";


    private final String serviceName;
    private final String serviceType;
    private final UUID serviceId;
    private final String announcementName;
    private final boolean staticAnnouncement;

    @JsonProperty(value="properties")
    private final Map<String, String> grabBag = Maps.newHashMap();

    public static final ServiceInformation forService(final String serviceName, final String serviceType, final String serviceScheme, final String serviceAddress, final int port)
    {
        return new ServiceInformation(serviceName,
                                      serviceType,
                                      null,
                                      ImmutableMap.of(PROP_SERVICE_SCHEME, serviceScheme,
                                                      PROP_SERVICE_ADDRESS, serviceAddress,
                                                      PROP_SERVICE_PORT, Integer.toString(port)));
    }

    public static ServiceInformation staticAnnouncement(final String serviceName, final String serviceType, final String serviceScheme, final String serviceAddress, final int port)
    {
        return staticAnnouncement(UUID.randomUUID(), serviceName, serviceType, serviceScheme, serviceAddress, port);
    }

    public static ServiceInformation staticAnnouncement(final UUID serviceId, final String serviceName, final String serviceType, final String serviceScheme, final String serviceAddress, final int port)
    {
        return new ServiceInformation(serviceName,
                serviceType,
                serviceId,
                ImmutableMap.of(PROP_SERVICE_SCHEME, serviceScheme,
                                PROP_SERVICE_ADDRESS, serviceAddress,
                                PROP_SERVICE_PORT, Integer.toString(port)),
                true);
    }

    public ServiceInformation(final String serviceName,
                              final String serviceType,
                              final UUID serviceId,
                              final Map<String, String> grabBag)
    {
        this(serviceName, serviceType, serviceId, grabBag, false);
    }

    @JsonCreator
    ServiceInformation(@Nonnull  @JsonProperty("serviceName") final String serviceName,
            @Nullable @JsonProperty("serviceType") final String serviceType,
            @JsonProperty("serviceId") final UUID serviceId,
            @JsonProperty("properties") final Map<String, String> grabBag,
            @JsonProperty("staticAnnouncement") final Boolean staticAnnouncement)
    {
        Preconditions.checkNotNull(serviceName, "Service name can not be null!");

        this.serviceName = serviceName;
        this.serviceType = serviceType;
        this.serviceId = serviceId == null ? UUID.randomUUID(): serviceId;
        this.staticAnnouncement = BooleanUtils.isTrue(staticAnnouncement);

        if (grabBag != null) {
            this.grabBag.putAll(grabBag);
        }

        this.announcementName = StringUtils.join(new String [] {this.serviceName, this.serviceType, this.serviceId.toString()}, "-");
    }

    @JsonProperty
    @Nonnull
    public String getServiceName()
    {
        return serviceName;
    }

    @JsonProperty
    @Nullable
    public String getServiceType()
    {
        return serviceType;
    }

    @JsonProperty
    public UUID getServiceId()
    {
        return serviceId;
    }

    @JsonIgnore
    public String getProperty(final String propertyName)
    {
        return grabBag.get(propertyName);
    }

    @JsonIgnore
    public String getAnnouncementName()
    {
        return announcementName;
    }

    @JsonProperty
    public boolean isStaticAnnouncement()
    {
        return staticAnnouncement;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof ServiceInformation))
            return false;
        final ServiceInformation castOther = (ServiceInformation) other;
        return new EqualsBuilder().append(serviceName, castOther.serviceName)
            .append(serviceType, castOther.serviceType)
            .append(serviceId, castOther.serviceId)
            .append(announcementName, castOther.announcementName)
            .append(grabBag, castOther.grabBag)
            .append(staticAnnouncement, castOther.staticAnnouncement)
            .isEquals();
    }

    private transient int hashCode;

    @Override
    public int hashCode()
    {
        if (hashCode == 0) {
            hashCode = new HashCodeBuilder().append(serviceName).append(serviceType).append(serviceId).append(announcementName).append(grabBag).append(staticAnnouncement).toHashCode();
        }
        return hashCode;
    }

    private transient String toString;

    @Override
    public String toString()
    {
        if (toString == null) {
            toString = new ToStringBuilder(this).append("serviceName", serviceName).append("serviceType", serviceType).append("serviceId", serviceId).append("announcementName", announcementName).append("grabBag", grabBag).append("staticAnnouncement", staticAnnouncement).toString();
        }
        return toString;
    }
}
