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
package ness.discovery.client.internal;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.annotations.VisibleForTesting;

/**
 * Maintains the state of the world (i.e. the service discovery map). The map can only be changed atomically, also it is possible to wait on the
 * first state change to occur.
 */
public class StateOfTheWorldHolder
{
    private final AtomicReference<Map<String, ConsistentRingGroup>> stateOfTheWorldHolder = new AtomicReference<Map<String, ConsistentRingGroup>>(Collections.<String, ConsistentRingGroup>emptyMap());

    private final CountDownLatch worldChangeWaitingLock;

    StateOfTheWorldHolder(final boolean blocking)
    {
        // Only wait for the first service update to happen if service discovery is actually enabled.
        this.worldChangeWaitingLock = new CountDownLatch(blocking ? 1 : 0);
    }

    @VisibleForTesting
    public void setState(final Map<String, ConsistentRingGroup> newWorldOrder)
    {
        stateOfTheWorldHolder.set(newWorldOrder);
        worldChangeWaitingLock.countDown();
    }

    public Map<String, ConsistentRingGroup> getState()
    {
        return stateOfTheWorldHolder.get();
    }

    void waitForWorldChange() throws InterruptedException
    {
        worldChangeWaitingLock.await();
    }

    boolean waitForWorldChange(final long timeout, final TimeUnit timeUnit) throws InterruptedException
    {
        return worldChangeWaitingLock.await(timeout, timeUnit);
    }
}


