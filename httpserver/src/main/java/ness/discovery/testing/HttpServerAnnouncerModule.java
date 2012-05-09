package ness.discovery.testing;

import ness.discovery.announce.ServiceAnnouncementFactory;
import ness.discovery.client.DiscoveryClient;
import ness.discovery.client.ServiceInformation;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nesscomputing.httpserver.HttpServer;
import com.nesscomputing.lifecycle.LifecycleStage;
import com.nesscomputing.lifecycle.guice.OnStage;

class HttpServerAnnouncerModule extends AbstractModule {

    @Override
    protected void configure() {
        bind (Announcer.class).asEagerSingleton();
        bind (ServiceAnnouncementFactory.class);
    }


    static class Announcer {
        private final HttpServer httpServer;
        private final DiscoveryClient discoveryClient;
        private ServiceInformation serviceInfo;
        private final String serviceName;

        @Inject
        Announcer(HttpServer httpServer, DiscoveryClient discoveryClient, @Named("SERVICE") String serviceName) {
            this.httpServer = httpServer;
            this.discoveryClient = discoveryClient;
            this.serviceName = serviceName;
        }

        @OnStage(LifecycleStage.ANNOUNCE)
        public void announce() {
            serviceInfo = ServiceInformation.forService(serviceName, null, "http", "localhost", httpServer.getInternalHttpPort());
            discoveryClient.announce(serviceInfo);
        }

        @OnStage(LifecycleStage.STOP)
        public void unannounce() {
            if (serviceInfo != null) {
                discoveryClient.unannounce(serviceInfo);
                serviceInfo = null;
            }
        }
    }
}
