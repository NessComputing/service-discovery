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

/**
 * Opaque directive to the {@link ServiceLocator} which affects the
 * service discovery process.
 *
 * @author steven
 */
public class ServiceHint {
    public static final String VERSION_HINT = "Version";
    public static final String QUALIFIER_HINT = "Qualifier";
    public static final String CONSISTENTHASH_HINT = "ConsistentHash";
    private final String name;
    private final String value;

    ServiceHint(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    /*
     * Factory methods
     */

    /**
     * Select a service with a particular qualifier
     */
    public static ServiceHint withQualifier(String qualifier) {
        return new ServiceHint(QUALIFIER_HINT, qualifier);
    }

    public static ServiceHint withVersion(int version) {
        return new ServiceHint(VERSION_HINT, String.valueOf(version));
    }

    /** Selects a service that serves the requested hash key.
     *
     * @param hashKey (example: user id, or another consistent identifier)
     * @return
     */
    public static ServiceHint servesKey(String hashKey) {
        return new ServiceHint(CONSISTENTHASH_HINT, hashKey);
    }
}
