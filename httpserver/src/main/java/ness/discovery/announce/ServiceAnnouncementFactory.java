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
package ness.discovery.announce;

import ness.discovery.client.ServiceInformation;
import ness.discovery.client.ServiceInformationBuilder;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nesscomputing.httpserver.HttpServer;

@Singleton
public class ServiceAnnouncementFactory {
    private final HttpServer httpServer;

    @Inject
    ServiceAnnouncementFactory(HttpServer httpServer) {
        this.httpServer = httpServer;
    }

    public ServiceInformationBuilder newBuilder() {
        String internalAddress = httpServer.getInternalAddress();
        int internalPort = httpServer.getInternalHttpPort();

        Preconditions.checkState(!StringUtils.isBlank(internalAddress), "blank internal address");
        Preconditions.checkState(internalPort > 0, "unconfigured internal http port");

        return new ServiceInformationBuilder()
            .putGrabBag(ServiceInformation.PROP_SERVICE_SCHEME, "http")
            .putGrabBag(ServiceInformation.PROP_SERVICE_ADDRESS, internalAddress)
            .putGrabBag(ServiceInformation.PROP_SERVICE_PORT, Integer.toString(internalPort));
    }
}
