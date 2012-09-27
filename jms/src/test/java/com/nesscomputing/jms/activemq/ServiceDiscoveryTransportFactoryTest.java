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
package com.nesscomputing.jms.activemq;

import java.util.UUID;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;


import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.lang3.ObjectUtils;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nesscomputing.config.Config;
import com.nesscomputing.config.ConfigModule;
import com.nesscomputing.jms.JmsModule;
import com.nesscomputing.logging.Log;
import com.nesscomputing.service.discovery.client.ReadOnlyDiscoveryClient;
import com.nesscomputing.service.discovery.client.ServiceInformation;
import com.nesscomputing.service.discovery.testing.client.MockedReadOnlyDiscoveryClient;
import com.nesscomputing.testing.lessio.AllowDNSResolution;

@AllowDNSResolution
public class ServiceDiscoveryTransportFactoryTest {
    private static final String QNAME = "disco-test-queue";
    private static final Log LOG = Log.findLog();
    String uniqueId = UUID.randomUUID().toString();

    @Inject
    @Named("test")
    ConnectionFactory factory;

    @Test
    public void testDiscoveryUri() throws Exception {
        final ServiceInformation vmbrokerInfo = new ServiceInformation("vmbroker", null, UUID.randomUUID(),
                ImmutableMap.of("uri", "vm://disco-test-broker-" + uniqueId));

        final Config config = Config.getFixedConfig(ImmutableMap.of("ness.jms.connection-url", "srvc://vmbroker?discoveryId=%s"));

        Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                binder().requireExplicitBindings();
                binder().disableCircularProxies();

                install (new ConfigModule(config));
                install (new JmsModule(config, "test"));
                install (new DiscoveryJmsModule(config));

                bind (ReadOnlyDiscoveryClient.class).toInstance(
                    MockedReadOnlyDiscoveryClient.builder().addServiceInformation(vmbrokerInfo).build());
            }
        }).injectMembers(this);

        final ConnectionFactory directFactory = new ActiveMQConnectionFactory("vm://disco-test-broker-" + uniqueId + "?broker.persistent=false");

        final Connection directConnection = directFactory.createConnection();
        directConnection.start();
        try {
            sendTestMessage(directConnection);
            consumeTestMessage();
        } finally {
            directConnection.stop();
            directConnection.close();
        }
    }

    private void consumeTestMessage() throws Exception {
        final Connection connection = factory.createConnection();
        connection.start();
        try {
            final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            final MessageConsumer consumer = session.createConsumer(session.createQueue(QNAME));
            final Message message = consumer.receive(1000);

            LOG.info(ObjectUtils.toString(message, "<no message>"));

            Assert.assertEquals(uniqueId, ((TextMessage) message).getText());
        } finally {
            connection.stop();
            connection.close();
        }
    }

    private void sendTestMessage(final Connection directConnection)
    throws Exception {
        final Session session = directConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        final MessageProducer producer = session.createProducer(session.createQueue(QNAME));
        producer.send(session.createTextMessage(uniqueId));
        session.close();
    }
}
