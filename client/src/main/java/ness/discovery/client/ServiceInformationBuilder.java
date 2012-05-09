package ness.discovery.client;

import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Maps;

public class ServiceInformationBuilder
{
    private String serviceName;
    private String serviceType;
    private UUID serviceId;
    private String announcementName;
    private final Map<String, String> grabBag = Maps.newHashMap();

    public ServiceInformationBuilder putGrabBag(String key, String value) {
        this.grabBag.put(key, value);
        return this;
    }

    public String getServiceName() {
        return serviceName;
    }

    public ServiceInformationBuilder setServiceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    public String getServiceType() {
        return serviceType;
    }

    public ServiceInformationBuilder setServiceType(String serviceType) {
        this.serviceType = serviceType;
        return this;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public ServiceInformationBuilder setServiceId(UUID serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public String getAnnouncementName() {
        return announcementName;
    }

    public ServiceInformationBuilder setAnnouncementName(String announcementName) {
        this.announcementName = announcementName;
        return this;
    }

    public Map<String, String> getGrabBag() {
        return grabBag;
    }

    public ServiceInformation build() {
        return new ServiceInformation(serviceName, serviceType, serviceId, grabBag);
    }
}


