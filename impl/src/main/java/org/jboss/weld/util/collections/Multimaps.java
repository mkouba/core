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
package org.jboss.weld.util.collections;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Multimap utilities.
 *
 * @author Jozef Hartinger
 *
 */
public class Multimaps {

    private Multimaps() {
    }

    private static final CacheBuilder<Object, Object> CACHE_BUILDER = CacheBuilder.newBuilder();

    private static class ConcurrentSetMultimapValueSupplier<K, V> extends CacheLoader<K, Set<V>> {
        @Override
        public Set<V> load(K input) {
            return Collections.synchronizedSet(new HashSet<V>());
        }
    }

    /**
     * Creates a {@link ConcurrentMap} instance whose values are populated with synchronized HashSet instances.
     */
    public static <K, V> LoadingCache<K, Set<V>> newConcurrentSetMultimap() {
        return CACHE_BUILDER.build(new ConcurrentSetMultimapValueSupplier<K, V>());
    }
}
