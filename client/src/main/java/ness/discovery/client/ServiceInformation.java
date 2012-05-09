package ness.discovery.client;

import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

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

    public ServiceInformation(@Nonnull  @JsonProperty("serviceName") final String serviceName,
                              @Nullable @JsonProperty("serviceType") final String serviceType,
                              @JsonProperty("serviceId") final UUID serviceId,
                              @JsonProperty("properties") final Map<String, String> grabBag)
    {
        Preconditions.checkNotNull(serviceName, "Service name can not be null!");

        this.serviceName = serviceName;
        this.serviceType = serviceType;
        this.serviceId = serviceId == null ? UUID.randomUUID(): serviceId;

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

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof ServiceInformation))
            return false;
        ServiceInformation castOther = (ServiceInformation) other;
        return new EqualsBuilder().append(serviceName, castOther.serviceName)
            .append(serviceType, castOther.serviceType)
            .append(serviceId, castOther.serviceId)
            .append(announcementName, castOther.announcementName)
            .append(grabBag, castOther.grabBag)
            .isEquals();
    }

    private transient int hashCode;

    @Override
    public int hashCode()
    {
        if (hashCode == 0) {
            hashCode = new HashCodeBuilder().append(serviceName).append(serviceType).append(serviceId).append(announcementName).append(grabBag).toHashCode();
        }
        return hashCode;
    }

    private transient String toString;

    @Override
    public String toString()
    {
        if (toString == null) {
            toString = new ToStringBuilder(this).append("serviceName", serviceName).append("serviceType", serviceType).append("serviceId", serviceId).append("announcementName", announcementName).append("grabBag", grabBag).toString();
        }
        return toString;
    }


}


