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
package com.nesscomputing.service.discovery.job;

import java.io.IOException;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

/**
 * An unit of work that needs to be executed with a valid zookeeper connection.
 */
public abstract class ZookeeperJob
{
    public ZookeeperJob()
    {
    }

    /**
     * Execute the unit of work. The zookeeper is connected when this method is called but can
     * be disconnected at any time.
     *
     * @return True if the work was successful, false if not, then it might be retried.
     */
    protected abstract boolean execute(final ZooKeeper zookeeper)
        throws KeeperException, IOException;
}
