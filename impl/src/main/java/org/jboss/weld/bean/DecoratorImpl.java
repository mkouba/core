/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat, Inc., and individual contributors
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
package org.jboss.weld.bean;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.Decorator;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.annotated.enhanced.MethodSignature;
import org.jboss.weld.annotated.runtime.InvokableAnnotatedMethod;
import org.jboss.weld.bootstrap.BeanDeployerEnvironment;
import org.jboss.weld.injection.attributes.WeldInjectionPointAttributes;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.SharedObjectCache;
import org.jboss.weld.util.Decorators;
import org.jboss.weld.util.reflection.Formats;

public class DecoratorImpl<T> extends ManagedBean<T> implements WeldDecorator<T> {

    /**
     * Creates a decorator bean
     *
     * @param <T>         The type
     * @param clazz       The class
     * @param beanManager the current manager
     * @return a Bean
     */
    public static <T> DecoratorImpl<T> of(BeanAttributes<T> attributes, EnhancedAnnotatedType<T> clazz, BeanManagerImpl beanManager) {
        return new DecoratorImpl<T>(attributes, clazz, beanManager);
    }

    private Map<MethodSignature, InvokableAnnotatedMethod<?>> decoratorMethods;
    private WeldInjectionPointAttributes<?, ?> delegateInjectionPoint;
    private Set<Annotation> delegateBindings;
    private Type delegateType;
    private Set<Type> delegateTypes;
    private Set<Type> decoratedTypes;

    protected DecoratorImpl(BeanAttributes<T> attributes, EnhancedAnnotatedType<T> type, BeanManagerImpl beanManager) {
        super(attributes, type, Decorator.class.getSimpleName() + BEAN_ID_SEPARATOR + type.getName(), beanManager);
    }

    @Override
    public void internalInitialize(BeanDeployerEnvironment environment) {
        super.internalInitialize(environment);
        initDelegateInjectionPoint();
        initDecoratedTypes();
        initDelegateBindings();
        initDelegateType();
    }

    protected void initDecoratedTypes() {
        Set<Type> decoratedTypes = new HashSet<Type>(getEnhancedAnnotated().getInterfaceClosure());
        decoratedTypes.retainAll(getTypes());
        decoratedTypes.remove(Serializable.class);
        this.decoratedTypes = SharedObjectCache.instance(beanManager).getSharedSet(decoratedTypes);
        this.decoratorMethods = Decorators.getDecoratorMethods(beanManager, decoratedTypes, getEnhancedAnnotated());
    }

    protected void initDelegateInjectionPoint() {
        // TODO: findDelegateInjectionPoint() is called also from DecoratorInjectionTarget. Try to avoid calling the method multiple times
        this.delegateInjectionPoint = Decorators.findDelegateInjectionPoint(getEnhancedAnnotated(), getInjectionPoints());
    }

    protected void initDelegateBindings() {
        this.delegateBindings = new HashSet<Annotation>();
        this.delegateBindings.addAll(this.delegateInjectionPoint.getQualifiers());
    }

    protected void initDelegateType() {
        this.delegateType = this.delegateInjectionPoint.getType();
        this.delegateTypes = new HashSet<Type>();
        delegateTypes.add(delegateType);
    }

    public Set<Annotation> getDelegateQualifiers() {
        return delegateBindings;
    }

    public Type getDelegateType() {
        return delegateType;
    }

    public Set<Type> getDecoratedTypes() {
        return decoratedTypes;
    }

    public WeldInjectionPointAttributes<?, ?> getDelegateInjectionPoint() {
        return delegateInjectionPoint;
    }

    public InvokableAnnotatedMethod<?> getDecoratorMethod(Method method) {
        return Decorators.findDecoratorMethod(this, decoratorMethods, method);
    }

    @Override
    public String toString() {
        return "Decorator [" + getBeanClass().toString() + "] decorates [" + Formats.formatTypes(getDecoratedTypes()) + "] with delegate type [" + Formats.formatType(getDelegateType()) + "] and delegate qualifiers [" + Formats.formatAnnotations(getDelegateQualifiers()) + "]";
    }
}
