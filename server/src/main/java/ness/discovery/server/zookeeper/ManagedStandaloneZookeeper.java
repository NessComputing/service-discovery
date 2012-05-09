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
package ness.discovery.server.zookeeper;

import com.nesscomputing.lifecycle.Lifecycle;
import com.nesscomputing.lifecycle.LifecycleListener;
import com.nesscomputing.lifecycle.LifecycleStage;

import java.io.IOException;

import org.apache.zookeeper.server.NIOServerCnxn;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;

import com.google.common.base.Throwables;
import com.google.inject.Inject;

/**
 * Single node, standalone zookeeper server. Should be only used for testing.
 */
public class ManagedStandaloneZookeeper
{
    private final ZooKeeperServer zookeeperServer;
    private final NIOServerCnxn.Factory cnxnFactory;

    @Inject
    ManagedStandaloneZookeeper(final QuorumPeerConfig quorumPeerConfig,
                               final NIOServerCnxn.Factory cnxnFactory,
                               final FileTxnSnapLog fileTxnSnapLog)
    {
        zookeeperServer = new ZooKeeperServer();
        this.cnxnFactory = cnxnFactory;

        zookeeperServer.setTxnLogFactory(fileTxnSnapLog);
        zookeeperServer.setTickTime(quorumPeerConfig.getTickTime());
        zookeeperServer.setMinSessionTimeout(quorumPeerConfig.getMinSessionTimeout());
        zookeeperServer.setMaxSessionTimeout(quorumPeerConfig.getMaxSessionTimeout());
    }

    @Inject(optional=true)
    void injectLifecycle(final Lifecycle lifecycle)
    {
        lifecycle.addListener(LifecycleStage.START_STAGE, new LifecycleListener() {
            @Override
            public void onStage(final LifecycleStage stage) {
                try {
                    cnxnFactory.startup(zookeeperServer);
                }
                catch (IOException ioe) {
                    throw Throwables.propagate(ioe);
                }
                catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw Throwables.propagate(ie);
                }
            }
        });

        lifecycle.addListener(LifecycleStage.STOP_STAGE, new LifecycleListener() {
            @Override
            public void onStage(final LifecycleStage stage) {
                cnxnFactory.shutdown();
                if (zookeeperServer.isRunning()) {
                    zookeeperServer.shutdown();
                }
                try {
                    cnxnFactory.join();
                }
                catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }
}
