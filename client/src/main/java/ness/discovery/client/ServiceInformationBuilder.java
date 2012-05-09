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


