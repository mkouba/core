/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.weld.tests.interceptors.producer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InterceptionFactory;

@ApplicationScoped
public class Producer {

    static final List<String> INVOCATIONS = new ArrayList<>();

    @Produced
    @Dependent
    @Produces
    public Foo produceFoo(InterceptionFactory<Foo> interceptionFactory) {
        interceptionFactory.configure()
                .filterMethods((m) -> m.getJavaMember().getName().equals("ping") && m.getJavaMember().getParameterCount() == 0)
                .findFirst().get().add(Hello.Literal.INSTANCE);
        return interceptionFactory.createInterceptedInstance(new Foo());
    }

    @Produced
    @Dependent
    @Produces
    public Map<String, Object> produceMap(InterceptionFactory<HashMap<String, Object>> interceptionFactory) {
        interceptionFactory.ignoreFinalMethods().configure().filterMethods((m) -> {
            if (m.getJavaMember().getDeclaringClass().equals(HashMap.class) && m.getJavaMember().getName().equals("put")
                    && m.getJavaMember().getParameterCount() == 2) {
                return true;
            }
            return false;
        }).findFirst().get().add(Monitor.Literal.INSTANCE);
        return interceptionFactory.createInterceptedInstance(new HashMap<>());
    }

    static class Foo {

        String ping() {
            return "pong";
        }

    }

    static void reset() {
        INVOCATIONS.clear();
    }
}
