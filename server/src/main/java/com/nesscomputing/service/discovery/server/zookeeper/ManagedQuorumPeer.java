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
package com.nesscomputing.service.discovery.server.zookeeper;

import com.nesscomputing.lifecycle.Lifecycle;
import com.nesscomputing.lifecycle.LifecycleListener;
import com.nesscomputing.lifecycle.LifecycleStage;

import org.apache.zookeeper.server.NIOServerCnxn;
import org.apache.zookeeper.server.ZKDatabase;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.server.quorum.QuorumPeer;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;

import com.google.inject.Inject;

/**
 * Clustered, quorum peered Zookeeper server. This is used for production.
 */
public class ManagedQuorumPeer
{
    private final QuorumPeer quorumPeer;

    @Inject
    ManagedQuorumPeer(final QuorumPeerConfig quorumPeerConfig,
                      final NIOServerCnxn.Factory cnxnFactory,
                      final FileTxnSnapLog fileTxnSnapLog)
    {
        quorumPeer = new QuorumPeer();
        quorumPeer.setClientPortAddress(quorumPeerConfig.getClientPortAddress());
        quorumPeer.setTxnFactory(fileTxnSnapLog);
        quorumPeer.setQuorumPeers(quorumPeerConfig.getServers());
        quorumPeer.setElectionType(quorumPeerConfig.getElectionAlg());
        quorumPeer.setMyid(quorumPeerConfig.getServerId());
        quorumPeer.setTickTime(quorumPeerConfig.getTickTime());
        quorumPeer.setMinSessionTimeout(quorumPeerConfig.getMinSessionTimeout());
        quorumPeer.setMaxSessionTimeout(quorumPeerConfig.getMaxSessionTimeout());
        quorumPeer.setInitLimit(quorumPeerConfig.getInitLimit());
        quorumPeer.setSyncLimit(quorumPeerConfig.getSyncLimit());
        quorumPeer.setQuorumVerifier(quorumPeerConfig.getQuorumVerifier());
        quorumPeer.setCnxnFactory(cnxnFactory);
        quorumPeer.setZKDatabase(new ZKDatabase(quorumPeer.getTxnFactory()));
        quorumPeer.setLearnerType(quorumPeerConfig.getPeerType());
    }

    @Inject(optional=true)
    void injectLifecycle(final Lifecycle lifecycle)
    {
        lifecycle.addListener(LifecycleStage.START_STAGE, new LifecycleListener() {
            @Override
            public void onStage(final LifecycleStage stage) {
                quorumPeer.start();
            }
        });

        lifecycle.addListener(LifecycleStage.STOP_STAGE, new LifecycleListener() {
            @Override
            public void onStage(final LifecycleStage stage) {
                quorumPeer.shutdown();
                try {
                    quorumPeer.join();
                }
                catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }
}
