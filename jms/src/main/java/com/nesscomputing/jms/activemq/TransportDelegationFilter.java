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

import java.lang.reflect.Method;

import org.apache.activemq.transport.Transport;

import com.google.common.base.Throwables;
import com.nesscomputing.logging.Log;

import net.sf.cglib.proxy.Dispatcher;
import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 * CGLIB filter which ignores all methods that are on a specified interface
 * and delegates the remaining methods to a given object.
 */
class TransportDelegationFilter implements CallbackFilter {
    private static final Log LOG = Log.findLog();

    private final Transport actualTransport;

    private final Class<?> beanClass;

    TransportDelegationFilter(Transport actualTransport, Class<?> beanClass) {
        this.actualTransport = actualTransport;
        this.beanClass = beanClass;
    }

    private final Callback[] callbacks = new Callback[] {
            new PassthruCallback(),
            new NoOpCallback()
    };

    public Callback[] getCallbacks() {
        return callbacks;
    }

    @Override
    public int accept(Method method) {
        try {
            // We only care if the method exists, but there seems to not be a better way to ask if an object responds to a method.
            // This is all done at class creation time so the overhead of a few exceptions should not be onerous.
            beanClass.getMethod(method.getName(), method.getParameterTypes());
            // Found on the ignore class, so return the index of the NoOpCallback
            return 1;
        } catch (SecurityException e) {
            Throwables.propagate(e);
        } catch (NoSuchMethodException e) {
            // Fall through
            LOG.trace(e);
        }

        // Not found on the ignore class, so return the index of the PassthruCallback
        return 0;
    }

    private class PassthruCallback implements Dispatcher {
        @Override
        public Object loadObject() throws Exception {
            return actualTransport;
        }
    }

    private static class NoOpCallback implements MethodInterceptor {
        @Override
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
            return null;
        }
    }
}
