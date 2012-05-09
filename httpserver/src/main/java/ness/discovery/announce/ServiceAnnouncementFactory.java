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
