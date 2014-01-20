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
package org.jboss.weld.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedMethod;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.injection.InjectionPointFactory;
import org.jboss.weld.injection.MethodInjectionPoint;
import org.jboss.weld.logging.BeanLogger;
import org.jboss.weld.logging.EventLogger;
import org.jboss.weld.logging.UtilLogger;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.util.collections.ArraySet;
import org.jboss.weld.util.collections.WeldCollections;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

public class BeanMethods {

    @SuppressWarnings("rawtypes")
    private static final Predicate<EnhancedAnnotatedMethod> BRIDGE_METHOD_FILTER_PREDICATE = new Predicate<EnhancedAnnotatedMethod>() {
        @Override
        public boolean apply(EnhancedAnnotatedMethod method) {
            return !method.getJavaMember().isBridge();
        }
    };

    private BeanMethods() {
    }

    /**
     * We need to employ different strategies when discovering a list of specific methods of a {@link Bean} (e.g. initializer
     * methods, producer methods, lifecycle event callback listeners, etc.) An implementation of this interface knows how to
     * establish a list of certain methods.
     *
     * The user of this implementation starts interaction by calling {@link #getAllMethods(EnhancedAnnotatedType)} which obtains
     * a collection of all methods of a given kind. Afterwards, iterates over the class hierarchy of
     * {@link AnnotatedType#getJavaClass()} from the most specific class to {@link Object}. For each class in the hierarchy it
     * calls {@link #levelStart(Class)}. Then it calls {@link #processMethod(EnhancedAnnotatedMethod)} for each method out of
     * the collection of all methods of the given kind (see above) which is declared by the current class (current level). Once
     * all the methods declared by the current class are processed, {@link #levelFinish()} is called and the iteration may
     * continue with a superclass of the current class (provided there is any).
     *
     * Finally, {@link #create()} is called to obtain the result.
     *
     * @author Jozef Hartinger
     *
     * @param <T> the class declaring the annotated type
     * @param <R> type of result (e.g. a list of AnnotatedMethods, a list of sets of AnnotatedMethods, etc.)
     */
    private interface MethodListBuilder<T, R> {

        /**
         * Returns all methods of a given kind (e.g. all observer methods) of a given {@link EnhancedAnnotatedType}. This
         * includes methods defined on classes upper in the class hierarchy. Overridden methods are not returned.
         */
        Collection<EnhancedAnnotatedMethod<?, ? super T>> getAllMethods(EnhancedAnnotatedType<T> type);

        /**
         * This method is called before methods declared by a specific class in the class hierarchy are processed. Only classes
         * declared by this specific class are processed until {@link #levelFinish()} is called.
         */
        void levelStart(Class<? super T> clazz);

        /**
         * Allows an implementation to process a method. By default the method would be added to the list of methods being
         * built.
         */
        void processMethod(EnhancedAnnotatedMethod<?, ? super T> method);

        /**
         * Indicates that processing of methods declared by a given class has ended. There are no more methods declared by the
         * given class to be processed.
         */
        void levelFinish();

        /**
         * Obtains the result. This method may not be idempotent and it is therefore not safe to call it multiple times.
         *
         * @return the list of methods of a given kind
         */
        R create();
    }

    /**
     * Get all methods of a given kind using a given {@link MethodListBuilder}.
     */
    private static <T, R> R getMethods(EnhancedAnnotatedType<T> type, MethodListBuilder<T, R> builder) {
        Collection<EnhancedAnnotatedMethod<?, ? super T>> methods = filterOutBridgeMethods(builder.getAllMethods(type));
        for (Class<? super T> clazz = type.getJavaClass(); clazz != null && clazz != Object.class; clazz = clazz
                .getSuperclass()) {
            builder.levelStart(clazz);
            for (EnhancedAnnotatedMethod<?, ? super T> method : methods) {
                if (method.getJavaMember().getDeclaringClass().equals(clazz)) {
                    builder.processMethod(method);
                }
            }
            builder.levelFinish();
        }
        return builder.create();
    }

    /**
     * For lifecycle event callback we need an ordered list. Lifecycle callback methods defined on the most specific class go
     * first in the list. A given class in the class hierarchy may define at most one method.
     *
     * @author Jozef Hartinger
     */
    private abstract static class AbstractLifecycleEventCallbackMethodListBuilder<T> implements
            MethodListBuilder<T, List<AnnotatedMethod<? super T>>> {

        protected List<AnnotatedMethod<? super T>> result = new ArrayList<AnnotatedMethod<? super T>>();
        protected EnhancedAnnotatedMethod<?, ? super T> foundMethod = null;

        @Override
        public void levelStart(Class<? super T> clazz) {
            foundMethod = null;
        }

        @Override
        public void processMethod(EnhancedAnnotatedMethod<?, ? super T> method) {
            if (methodHasNoParameters(method)) {
                if (foundMethod != null) {
                    duplicateMethod(method);
                }
                foundMethod = method;
            }
        }

        private boolean methodHasNoParameters(EnhancedAnnotatedMethod<?, ? super T> method) {
            return method.getParameterTypesAsArray().length == 0;
        }

        @Override
        public void levelFinish() {
            if (foundMethod != null) {
                result.add(processLevelResult(foundMethod).slim());
            }
        }

        @Override
        public List<AnnotatedMethod<? super T>> create() {
            Collections.reverse(result);
            return WeldCollections.immutableList(result);
        }

        /**
         * Called when a given hierarchy level defines multiple lifecycle event callback of a given type.
         */
        protected abstract void duplicateMethod(EnhancedAnnotatedMethod<?, ? super T> method);

        /**
         * Called when a unique method is found for a given hierarchy level.
         */
        protected abstract EnhancedAnnotatedMethod<?, ? super T> processLevelResult(EnhancedAnnotatedMethod<?, ? super T> method);
    }

    /**
     * For initializers we need to return a {@link List} of {@link Set}s of initializer methods (because a class may declare
     * multiple initializers). The list is ordered the same way as for lifecycle event callbacks.
     *
     * @author Jozef Hartinger
     */
    private static class InitializerMethodListBuilder<T> implements MethodListBuilder<T, List<Set<MethodInjectionPoint<?, ?>>>> {

        private final List<Set<MethodInjectionPoint<?, ?>>> result = new ArrayList<Set<MethodInjectionPoint<?, ?>>>();
        private Set<MethodInjectionPoint<?, ?>> currentLevel = null;

        private final EnhancedAnnotatedType<T> type;
        private final BeanManagerImpl manager;
        private final Bean<?> declaringBean;

        public InitializerMethodListBuilder(EnhancedAnnotatedType<T> type, Bean<?> declaringBean, BeanManagerImpl manager) {
            this.type = type;
            this.manager = manager;
            this.declaringBean = declaringBean;
        }

        @Override
        public Collection<EnhancedAnnotatedMethod<?, ? super T>> getAllMethods(EnhancedAnnotatedType<T> type) {
            return type.getEnhancedMethods(Inject.class);
        }

        @Override
        public void levelStart(Class<? super T> clazz) {
            currentLevel = new ArraySet<MethodInjectionPoint<?, ?>>();
        }

        @Override
        public void processMethod(EnhancedAnnotatedMethod<?, ? super T> method) {
            if (method.isAnnotationPresent(Inject.class)) {
                if (method.getAnnotation(Produces.class) != null) {
                    throw UtilLogger.LOG.initializerCannotBeProducer(method, type);
                } else if (method.getEnhancedParameters(Disposes.class).size() > 0) {
                    throw UtilLogger.LOG.initializerCannotBeDisposalMethod(method, type);
                } else if (method.getEnhancedParameters(Observes.class).size() > 0) {
                    throw EventLogger.LOG.invalidInitializer(method);
                } else if (method.isGeneric()) {
                    throw UtilLogger.LOG.initializerMethodIsGeneric(method, type);
                }
                if (!method.isStatic()) {
                    currentLevel.add(InjectionPointFactory.instance().createMethodInjectionPoint(method, declaringBean,
                            type.getJavaClass(), false, manager));
                }
            }
        }

        @Override
        public void levelFinish() {
            result.add(currentLevel);
        }

        @Override
        public List<Set<MethodInjectionPoint<?, ?>>> create() {
            Collections.reverse(result); // because we want methods that are lower in the hierarchy to be called first
            return WeldCollections.immutableGuavaList(result);
        }
    }

    public static <T> List<AnnotatedMethod<? super T>> getPostConstructMethods(final EnhancedAnnotatedType<T> type) {
        return getMethods(type, new AbstractLifecycleEventCallbackMethodListBuilder<T>() {

            @Override
            public Collection<EnhancedAnnotatedMethod<?, ? super T>> getAllMethods(EnhancedAnnotatedType<T> type) {
                return type.getEnhancedMethods(PostConstruct.class);
            }

            @Override
            protected void duplicateMethod(EnhancedAnnotatedMethod<?, ? super T> method) {
                throw UtilLogger.LOG.tooManyPostConstructMethods(type);
            }

            @Override
            protected EnhancedAnnotatedMethod<?, ? super T> processLevelResult(EnhancedAnnotatedMethod<?, ? super T> method) {
                BeanLogger.LOG.foundOnePostConstructMethod(method, type);
                return method;
            }
        });
    }

    public static <T> List<AnnotatedMethod<? super T>> getPreDestroyMethods(final EnhancedAnnotatedType<T> type) {
        return getMethods(type, new AbstractLifecycleEventCallbackMethodListBuilder<T>() {

            @Override
            public Collection<EnhancedAnnotatedMethod<?, ? super T>> getAllMethods(EnhancedAnnotatedType<T> type) {
                return type.getEnhancedMethods(PreDestroy.class);
            }

            @Override
            protected void duplicateMethod(EnhancedAnnotatedMethod<?, ? super T> method) {
                throw UtilLogger.LOG.tooManyPreDestroyMethods(type);
            }

            @Override
            protected EnhancedAnnotatedMethod<?, ? super T> processLevelResult(EnhancedAnnotatedMethod<?, ? super T> method) {
                BeanLogger.LOG.foundOnePreDestroyMethod(method, type);
                return method;
            }
        });
    }

    public static <T> List<Set<MethodInjectionPoint<?, ?>>> getInitializerMethods(Bean<?> declaringBean, EnhancedAnnotatedType<T> type, BeanManagerImpl manager) {
        return getMethods(type, new InitializerMethodListBuilder<T>(type, declaringBean, manager));
    }

    public static <T> Collection<EnhancedAnnotatedMethod<?, ? super T>> getObserverMethods(final EnhancedAnnotatedType<T> type) {
        return filterOutBridgeMethods(type.getEnhancedMethodsWithAnnotatedParameters(Observes.class));
    }

    /**
     * Oracle JDK 8 compiler (unlike prev versions) generates bridge methods which have method and parameter annotations copied from the original method.
     * However such methods should not become observers, producers, disposers, initializers and lifecycle callbacks.
     *
     * @param methods
     * @return a collection view with bridge methods filtered out
     * @see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6695379
     */
    public static <T> Collection<EnhancedAnnotatedMethod<?, ? super T>> filterOutBridgeMethods(final Collection<EnhancedAnnotatedMethod<?, ? super T>> methods) {
        return Collections2.filter(methods, BRIDGE_METHOD_FILTER_PREDICATE);
    }
}
