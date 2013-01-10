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
package com.nesscomputing.service.discovery.client;

import java.util.concurrent.TimeUnit;

import org.skife.config.Config;
import org.skife.config.Default;
import org.skife.config.TimeSpan;

public abstract class DiscoveryClientConfig
{
    /**
     * Whether discovery is enabled or disabled. It is intentionally disabled by default which
     * reduces the footprint of infrastructure needed to spin up a service that incorporates SD
     * e.g. for testing. All production and staging environments have this parameter set to "true"
     * in their global configs, so all deployments have that enabled.
     */
    @Config("ness.discovery.enabled")
    @Default("false")
    public boolean isEnabled()
    {
        return false;
    }

    /**
     * Whether or not to announce configured services. Normally, the service announces if it is read/write, but
     * for testing (e.g. running additional instances in staging), this can be turned off.
     */
    @Config("ness.discovery.announce.enabled")
    @Default("true")
    public boolean isAnnounceEnabled()
    {
        return true;
    }

    /**
     * Root of the service discovery tree. Defaults to "/ness/srvc".
     */
    @Config("ness.discovery.root")
    @Default("/ness/srvc")
    public String getRoot()
    {
        return "/ness/srvc";
    }

    /**
     * Scan time for the client. This is the absolute maximum time
     * between syncing the local state with remote state, usually, these are triggered
     * by the watch on the root node. Default is 120 seconds.
     *
     */
    @Config("ness.discovery.scan-interval")
    @Default("120s")
    public TimeSpan getScanInterval()
    {
        return new TimeSpan(120, TimeUnit.SECONDS);
    }

    /**
     * Tick time of the internal thread. This is the interval used for checking internal
     * state changes. Default is 100 Milliseconds.
     *
     */
    @Config("ness.discovery.tick-interval")
    @Default("100ms")
    public TimeSpan getTickInterval()
    {
        return new TimeSpan(100, TimeUnit.MILLISECONDS);
    }

    /**
     * Default timeout for zookeeper related operations.
     */
    @Config("ness.discovery.zookeeper-timeout")
    @Default("500ms")
    public TimeSpan getZookeeperTimeout()
    {
        return new TimeSpan(500, TimeUnit.MILLISECONDS);
    }

    /**
     * Default penalty time for a bad node read from service discovery.
     */
    @Config("ness.discovery.penalty-time")
    @Default("600s")
    public TimeSpan getPenaltyTime()
    {
        return new TimeSpan(600, TimeUnit.SECONDS);
    }

    /**
     * Time to wait at startup for the first discovery world state before moving on with no known services.
     */
    @Config("ness.discovery.world-change-timeout")
    @Default("10s")
    public TimeSpan getWorldChangeTimeout()
    {
        return new TimeSpan("10s");
    }
}
