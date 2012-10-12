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
package com.nesscomputing.service.discovery.server.announce;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.lang3.StringUtils;

import com.nesscomputing.config.Config;
import com.nesscomputing.lifecycle.LifecycleStage;
import com.nesscomputing.lifecycle.guice.OnStage;
import com.nesscomputing.logging.Log;
import com.nesscomputing.service.discovery.client.DiscoveryClient;
import com.nesscomputing.service.discovery.client.ServiceInformation;

/**
 * Create static announcements from configuration.  Useful to announce resources that do not change very often,
 * for example a database.  Configuration keys are put in <code>ness.discovery.static-announce.&lt;service-id&gt;</code>.
 * Accepted keys are <code>name</code>, <code>type</code>, <code>scheme</code>, <code>address</code>, and <code>port</code>.
 * <p>For example: <br />
 * <code>
 * ness.discovery.static-announce.65f57132-8415-422e-b44a-7543dda54858.name=foo
 * ness.discovery.static-announce.65f57132-8415-422e-b44a-7543dda54858.scheme=http
 * ness.discovery.static-announce.65f57132-8415-422e-b44a-7543dda54858.address=127.0.0.1
 * ness.discovery.static-announce.65f57132-8415-422e-b44a-7543dda54858.port=12345
 * ness.discovery.static-announce.65f57132-8415-422e-b44a-7543dda54858.type=bar
 * </code>
 */
@Singleton
public class ConfigStaticAnnouncer
{
    private static final String CONFIG_ROOT = "ness.discovery.static-announce";
    private static final Log LOG = Log.findLog();
    private final DiscoveryClient discoveryClient;
    private final Config config;

    @Inject
    ConfigStaticAnnouncer(DiscoveryClient discoveryClient, Config config)
    {
        this.discoveryClient = discoveryClient;
        this.config = config;
    }

    @OnStage(LifecycleStage.ANNOUNCE)
    public void doStaticAnnounce()
    {
        final Map<UUID, Map<String, String>> configTree = getAnnouncements();

        for (final Entry<UUID, Map<String, String>> announce : configTree.entrySet()) {
            final Map<String, String> map = announce.getValue();
            final ServiceInformation info = ServiceInformation.staticAnnouncement(
                    announce.getKey(),
                    map.get("name"),
                    map.get("type"),
                    map.get("scheme"),
                    map.get("address"),
                    Integer.parseInt(map.get("port")));

            LOG.info("Configured static announcement: %s", announce);

            discoveryClient.announce(info);
        }
    }

    private Map<UUID, Map<String, String>> getAnnouncements()
    {
        final AbstractConfiguration subconfig = config.getConfiguration(CONFIG_ROOT);

        @SuppressWarnings("unchecked")
        final
        Iterator<String> keys = subconfig.getKeys();

        final Map<UUID, Map<String, String>> configTree = Maps.newHashMap();

        while (keys.hasNext()) {
            final String key = keys.next();

            final String id = StringUtils.substringBefore(key, ".");

            UUID serviceId;

            try {
                serviceId = UUID.fromString(id);
            } catch (final IllegalArgumentException e) {
                throw new IllegalArgumentException(String.format("Invalid serviceId \"%s\"", id), e);
            }

            Map<String, String> configMap = configTree.get(serviceId);
            if (configMap == null) {
                configTree.put(serviceId, configMap = Maps.newHashMap());
            }

            configMap.put(StringUtils.substringAfter(key, "."), subconfig.getString(key));
        }
        return configTree;
    }
}
