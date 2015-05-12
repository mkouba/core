/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.weld.bean.attributes;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.NormalScope;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.inject.Named;
import javax.inject.Qualifier;
import javax.inject.Scope;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotated;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedField;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedMember;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedMethod;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.ejb.InternalEjbDescriptor;
import org.jboss.weld.literal.AnyLiteral;
import org.jboss.weld.literal.DefaultLiteral;
import org.jboss.weld.literal.NamedLiteral;
import org.jboss.weld.literal.NewLiteral;
import org.jboss.weld.logging.BeanLogger;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.metadata.cache.MergedStereotypes;
import org.jboss.weld.resources.SharedObjectCache;
import org.jboss.weld.util.Beans;
import org.jboss.weld.util.collections.ArraySet;
import org.jboss.weld.util.reflection.Formats;
import org.jboss.weld.util.reflection.Reflections;

/**
 * Creates {@link BeanAttributes} based on a given annotated.
 *
 * @author Jozef Hartinger
 */
public class BeanAttributesFactory {

    private static final Set<Annotation> DEFAULT_QUALIFIERS = Collections.unmodifiableSet(new ArraySet<Annotation>(AnyLiteral.INSTANCE, DefaultLiteral.INSTANCE).trimToSize());

    private BeanAttributesFactory() {
    }

    /**
     * Creates new {@link BeanAttributes} to represent a managed bean.
     */
    public static <T> BeanAttributes<T> forBean(EnhancedAnnotated<T, ?> annotated, BeanManagerImpl manager) {
        return new BeanAttributesBuilder<T>(annotated, null, manager).build();
    }

    /**
     * Creates new {@link BeanAttributes} to represent a session bean.
     */
    public static <T> BeanAttributes<T> forSessionBean(EnhancedAnnotatedType<T> annotated, InternalEjbDescriptor<?> descriptor, BeanManagerImpl manager) {
        return new BeanAttributesBuilder<T>(annotated, Reflections.<InternalEjbDescriptor<T>> cast(descriptor), manager).build();
    }

    public static <T> BeanAttributes<T> forNewBean(Set<Type> types, final Class<?> javaClass) {
        Set<Annotation> qualifiers = Collections.<Annotation>singleton(new NewLiteral(javaClass));
        return new ImmutableBeanAttributes<T>(Collections.<Class<? extends Annotation>> emptySet(), false, null, qualifiers, types, Dependent.class);
    }

    public static <T> BeanAttributes<T> forNewManagedBean(EnhancedAnnotatedType<T> weldClass, BeanManagerImpl manager) {
        return forNewBean(SharedObjectCache.instance(manager).getSharedSet(Beans.getTypes(weldClass)), weldClass.getJavaClass());
    }

    public static <T> BeanAttributes<T> forNewSessionBean(BeanAttributes<T> originalAttributes, Class<?> javaClass) {
        return forNewBean(originalAttributes.getTypes(), javaClass);
    }

    private static class BeanAttributesBuilder<T> {

        private MergedStereotypes<T, ?> mergedStereotypes;
        private boolean alternative;
        private String name;
        private Set<Annotation> qualifiers;
        private Set<Type> types;
        private Class<? extends Annotation> scope;
        private BeanManagerImpl manager;
        protected final EnhancedAnnotated<T, ?> annotated;

        private BeanAttributesBuilder(EnhancedAnnotated<T, ?> annotated, InternalEjbDescriptor<T> descriptor, BeanManagerImpl manager) {
            this.manager = manager;
            this.annotated = annotated;
            initStereotypes(annotated, manager);
            initAlternative(annotated);
            initName(annotated);
            initQualifiers(annotated);
            initScope(annotated);
            if (descriptor == null) {
                types = SharedObjectCache.instance(manager).getSharedSet(Beans.getTypes(annotated));
            } else {
                types = SharedObjectCache.instance(manager).getSharedSet(Beans.getTypes(annotated, descriptor));
            }
        }

        protected <S> void initStereotypes(EnhancedAnnotated<T, S> annotated, BeanManagerImpl manager) {
            this.mergedStereotypes = MergedStereotypes.of(annotated, manager);
        }

        protected void initAlternative(EnhancedAnnotated<T, ?> annotated) {
            this.alternative = Beans.isAlternative(annotated, mergedStereotypes);
        }

        /**
         * Initializes the name
         */
        protected void initName(EnhancedAnnotated<T, ?> annotated) {
            boolean beanNameDefaulted = false;
            if (annotated.isAnnotationPresent(Named.class)) {
                String javaName = annotated.getAnnotation(Named.class).value();
                if ("".equals(javaName)) {
                    beanNameDefaulted = true;
                } else {
                    this.name = javaName;
                    return;
                }
            }
            if (beanNameDefaulted || (mergedStereotypes != null && mergedStereotypes.isBeanNameDefaulted())) {
                this.name = getDefaultName(annotated);
            }
        }

        /**
         * Gets the default name of the bean
         *
         * @return The default name
         */
        protected String getDefaultName(EnhancedAnnotated<?, ?> annotated) {
            if (annotated instanceof EnhancedAnnotatedType<?>) {
                return Introspector.decapitalize(((EnhancedAnnotatedType<?>) annotated).getSimpleName());
            } else if (annotated instanceof EnhancedAnnotatedField<?, ?>) {
                return ((EnhancedAnnotatedField<?, ?>) annotated).getPropertyName();
            } else if (annotated instanceof EnhancedAnnotatedMethod<?, ?>) {
                return ((EnhancedAnnotatedMethod<?, ?>) annotated).getPropertyName();
            } else {
                return null;
            }
        }

        protected void initQualifiers(Set<Annotation> qualifiers) {
            if (qualifiers.isEmpty()) {
                this.qualifiers = DEFAULT_QUALIFIERS;
            } else {
                ArraySet<Annotation> normalizedQualifiers = new ArraySet<Annotation>(qualifiers.size() + 2);
                if (qualifiers.size() == 1) {
                    if (qualifiers.iterator().next().annotationType().equals(Named.class)) {
                        normalizedQualifiers.add(DefaultLiteral.INSTANCE);
                    }
                }
                normalizedQualifiers.addAll(qualifiers);
                normalizedQualifiers.add(AnyLiteral.INSTANCE);
                if (name != null && normalizedQualifiers.remove(NamedLiteral.DEFAULT)) {
                    normalizedQualifiers.add(new NamedLiteral(name));
                }
                this.qualifiers = SharedObjectCache.instance(manager).getSharedSet(normalizedQualifiers);
            }
        }

        protected void initQualifiers(EnhancedAnnotated<?, ?> annotated) {
            initQualifiers(annotated.getMetaAnnotations(Qualifier.class));
        }

        protected void initScope(EnhancedAnnotated<T, ?> annotated) {
            // class bean
            if (annotated instanceof EnhancedAnnotatedType<?>) {
                EnhancedAnnotatedType<?> weldClass = (EnhancedAnnotatedType<?>) annotated;
                for (EnhancedAnnotatedType<?> clazz = weldClass; clazz != null; clazz = clazz.getEnhancedSuperclass()) {
                    Set<Annotation> scopes = new HashSet<Annotation>();
                    scopes.addAll(clazz.getDeclaredMetaAnnotations(Scope.class));
                    scopes.addAll(clazz.getDeclaredMetaAnnotations(NormalScope.class));
                    validateScopeSet(scopes, annotated);
                    if (scopes.size() == 1) {
                        if (annotated.isAnnotationPresent(scopes.iterator().next().annotationType())) {
                            this.scope = scopes.iterator().next().annotationType();
                        }
                        break;
                    }
                }
            } else {
                // producer field or method
                Set<Annotation> scopes = new HashSet<Annotation>();
                scopes.addAll(annotated.getMetaAnnotations(Scope.class));
                scopes.addAll(annotated.getMetaAnnotations(NormalScope.class));
                if (scopes.size() == 1) {
                    this.scope = scopes.iterator().next().annotationType();
                }
                validateScopeSet(scopes, annotated);
            }

            if (this.scope == null) {
                initScopeFromStereotype();
            }

            if (this.scope == null) {
                this.scope = Dependent.class;
            }
        }

        protected void validateScopeSet(Set<Annotation> scopes, EnhancedAnnotated<T, ?> annotated) {
            if (scopes.size() > 1) {
                throw BeanLogger.LOG.onlyOneScopeAllowed(annotated);
            }
        }

        protected boolean initScopeFromStereotype() {
            Set<Annotation> possibleScopes = mergedStereotypes.getPossibleScopes();
            if (possibleScopes.size() == 1) {
                this.scope = possibleScopes.iterator().next().annotationType();
                return true;
            } else if (possibleScopes.size() > 1) {
                String stack;
                Class<?> declaringClass;
                if (annotated instanceof EnhancedAnnotatedMember) {
                    EnhancedAnnotatedMember<?, ?, ?> member = (EnhancedAnnotatedMember<?, ?, ?>) annotated;
                    declaringClass = member.getDeclaringType().getJavaClass();
                    stack = "\n  at " + Formats.formatAsStackTraceElement(member.getJavaMember());
                } else {
                    declaringClass = annotated.getJavaClass();
                    stack = "";
                }
                throw BeanLogger.LOG.multipleScopesFoundFromStereotypes(Formats.formatType(declaringClass, false),
                        Formats.formatTypes(mergedStereotypes.getStereotypes(), false), possibleScopes, stack);
            } else {
                return false;
            }
        }

        public BeanAttributes<T> build() {
            return new ImmutableBeanAttributes<T>(mergedStereotypes.getStereotypes(), alternative, name, qualifiers, types, scope);
        }
    }
}
