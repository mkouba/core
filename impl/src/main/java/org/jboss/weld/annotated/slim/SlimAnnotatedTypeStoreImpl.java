/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.weld.annotated.slim;


import static org.jboss.weld.util.reflection.Reflections.cast;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jboss.weld.bootstrap.api.helpers.AbstractBootstrapService;

import com.google.common.base.Objects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class SlimAnnotatedTypeStoreImpl extends AbstractBootstrapService implements SlimAnnotatedTypeStore {

    private final LoadingCache<Class<?>, Set<SlimAnnotatedType<?>>> typesByClass;

    public SlimAnnotatedTypeStoreImpl() {
        this.typesByClass = CacheBuilder.newBuilder().build(new CacheLoader<Class<?>, Set<SlimAnnotatedType<?>>>() {
            @Override
            public Set<SlimAnnotatedType<?>> load(Class<?> input) {
                return new CopyOnWriteArraySet<SlimAnnotatedType<?>>();
            }
        });
    }

    @Override
    public <X> SlimAnnotatedType<X> get(Class<X> type, String suffix) {
        for (SlimAnnotatedType<X> annotatedType : get(type)) {
            if (Objects.equal(annotatedType.getIdentifier().getSuffix(), suffix)) {
                return annotatedType;
            }
        }
        return null;
    }

    @Override
    public <X> Set<SlimAnnotatedType<X>> get(Class<X> type) {
        return cast(Collections.unmodifiableSet(typesByClass.getUnchecked(type)));
    }

    @Override
    public <X> void put(SlimAnnotatedType<X> type) {
        typesByClass.getUnchecked(type.getJavaClass()).add(type);
    }

    @Override
    public void cleanupAfterBoot() {
        typesByClass.invalidateAll();
    }

}
