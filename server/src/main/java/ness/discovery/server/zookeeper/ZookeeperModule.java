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

import java.io.File;
import java.io.IOException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationConverter;
import org.apache.zookeeper.server.NIOServerCnxn;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig.ConfigException;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.ProvisionException;
import com.google.inject.Singleton;
import com.nesscomputing.config.Config;
import com.nesscomputing.logging.Log;

/**
 * Brings up a zookeeper server, guice style.
 */
public class ZookeeperModule extends AbstractModule
{
    private static final Log LOG = Log.findLog();

    private final Config config;

    public ZookeeperModule(final Config config)
    {
        this.config = config;
    }

    @Override
    public void configure()
    {
        final QuorumPeerConfig quorumPeerConfig = getQuorumPeerConfig();

        bind (QuorumPeerConfig.class).toInstance(quorumPeerConfig);
        final int servers = quorumPeerConfig.getServers().size();

        if (servers > 0) {
            LOG.info("Starting a quorum peer for a total number of %d servers!", servers);
            bind(ManagedQuorumPeer.class).asEagerSingleton();
        }
        else {
            LOG.info("Starting a standalone instance!");
            bind(ManagedStandaloneZookeeper.class).asEagerSingleton();
        }
    }

    private QuorumPeerConfig getQuorumPeerConfig()
    {
        final Configuration zookeeperConfig = config.getConfiguration("ness.zookeeper");
        final QuorumPeerConfig quorumPeerConfig = new QuorumPeerConfig();
        try {
            quorumPeerConfig.parseProperties(ConfigurationConverter.getProperties(zookeeperConfig));
        }
        catch (IOException ioe) {
            throw new ProvisionException("while creating the QuorumPeerConfig", ioe);
        }
        catch (ConfigException ce) {
            throw new ProvisionException("while creating the QuorumPeerConfig", ce);
        }
        return quorumPeerConfig;
    }

    @Provides
    @Singleton
    public NIOServerCnxn.Factory getConnectionFactory(final QuorumPeerConfig quorumPeerConfig) throws IOException
    {
        return new NIOServerCnxn.Factory(quorumPeerConfig.getClientPortAddress(), quorumPeerConfig.getMaxClientCnxns());
    }

    @Provides
    @Singleton
    public FileTxnSnapLog getFileTxnSnapLog(final QuorumPeerConfig quorumPeerConfig) throws IOException
    {
        return new FileTxnSnapLog(new File(quorumPeerConfig.getDataLogDir()), new File(quorumPeerConfig.getDataDir()));
    }
}
