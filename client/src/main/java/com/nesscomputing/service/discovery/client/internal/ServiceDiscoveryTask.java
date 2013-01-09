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
package com.nesscomputing.service.discovery.client.internal;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

import com.nesscomputing.service.discovery.client.DiscoveryClientConfig;

/**
 * Base class for a task run by the main service discovery thread.
 */
abstract class ServiceDiscoveryTask
{
    protected final DiscoveryClientConfig discoveryConfig;
    protected final ObjectMapper objectMapper;

    protected ServiceDiscoveryTask (final DiscoveryClientConfig discoveryConfig,
                                    final ObjectMapper objectMapper)
    {
        this.discoveryConfig = discoveryConfig;
        this.objectMapper = objectMapper;
    }

    protected final String getNodePath(final String nodeName)
    {
        return discoveryConfig.getRoot() + "/" + nodeName;
    }

    /**
     * Visit a list of nodes.
     */
    abstract void visit(final List<String> childNodes, final ZooKeeper zookeeper, final long tick) throws KeeperException, InterruptedException;

    /**
     * Trigger a scan of the nodes on zookeeper by incrementing the generation counter.
     */
    void determineGeneration(final AtomicLong generation, final long tick)
    {
        // Do nothing, derived classes override this.
    }
}
