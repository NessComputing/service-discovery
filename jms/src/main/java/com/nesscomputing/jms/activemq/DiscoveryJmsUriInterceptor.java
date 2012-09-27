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
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

import com.nesscomputing.jms.JmsUriInterceptor;

/**
 * Replace the single format specifier in a srvc:// URI with the unique ID specifying which
 * injector it belongs to.
 */
class DiscoveryJmsUriInterceptor implements JmsUriInterceptor {
    private final UUID injectorId;
    private final DiscoveryJmsConfig config;

    DiscoveryJmsUriInterceptor(DiscoveryJmsConfig config, UUID injectorId)
    {
        this.config = config;
        this.injectorId = injectorId;
    }

    @Override
    public String apply(String input)
    {
        Preconditions.checkState(config != null, "no config for %s", injectorId);

        if (!config.isSrvcTransportEnabled() || StringUtils.isEmpty(input))
        {
            return input;
        }

        return String.format(input, injectorId);
    }
}
