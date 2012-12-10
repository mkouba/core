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
package org.jboss.weld.manager;

import static org.jboss.weld.logging.messages.BeanManagerMessage.METHOD_NOT_AVAILABLE_DURING_INITIALIZATION;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.Producer;

import org.jboss.weld.Container;
import org.jboss.weld.ContainerState;
import org.jboss.weld.exceptions.IllegalStateException;

/**
 * Client view of {@link BeanManagerImpl}.
 *
 * @author Martin Kouba
 */
public abstract class ForwardingBeanManager implements BeanManager, Serializable {

    private static final long serialVersionUID = 8514246621564507610L;

    public abstract BeanManagerImpl delegate();

    @Override
    public Object getReference(Bean<?> bean, Type beanType, CreationalContext<?> ctx) {
        checkApplicationInitializationFinished("getReference()");
        return delegate().getReference(bean, beanType, ctx);
    }

    @Override
    public Object getInjectableReference(InjectionPoint ij, CreationalContext<?> ctx) {
        checkApplicationInitializationFinished("getInjectableReference()");
        return delegate().getInjectableReference(ij, ctx);
    }

    @Override
    public <T> CreationalContext<T> createCreationalContext(Contextual<T> contextual) {
        return delegate().createCreationalContext(contextual);
    }

    @Override
    public Set<Bean<?>> getBeans(Type beanType, Annotation... qualifiers) {
        checkApplicationInitializationFinished("getBeans()");
        return delegate().getBeans(beanType, qualifiers);
    }

    @Override
    public Set<Bean<?>> getBeans(String name) {
        checkApplicationInitializationFinished("getBeans()");
        return delegate().getBeans(name);
    }

    @Override
    public Bean<?> getPassivationCapableBean(String id) {
        checkApplicationInitializationFinished("getPassivationCapableBean()");
        return delegate().getPassivationCapableBean(id);
    }

    @Override
    public <X> Bean<? extends X> resolve(Set<Bean<? extends X>> beans) {
        checkApplicationInitializationFinished("resolve()");
        return delegate().resolve(beans);
    }

    @Override
    public void validate(InjectionPoint injectionPoint) {
        checkApplicationInitializationFinished("validate()");
        delegate().validate(injectionPoint);
    }

    @Override
    public void fireEvent(Object event, Annotation... qualifiers) {
        delegate().fireEvent(event, qualifiers);
    }

    @Override
    public <T> Set<ObserverMethod<? super T>> resolveObserverMethods(T event, Annotation... qualifiers) {
        checkApplicationInitializationFinished("resolveObserverMethods()");
        return delegate().resolveObserverMethods(event, qualifiers);
    }

    @Override
    public List<Decorator<?>> resolveDecorators(Set<Type> types, Annotation... qualifiers) {
        checkApplicationInitializationFinished("resolveDecorators()");
        return delegate().resolveDecorators(types, qualifiers);
    }

    @Override
    public List<Interceptor<?>> resolveInterceptors(InterceptionType type, Annotation... interceptorBindings) {
        checkApplicationInitializationFinished("resolveInterceptors()");
        return delegate().resolveInterceptors(type, interceptorBindings);
    }

    @Override
    public boolean isScope(Class<? extends Annotation> annotationType) {
        return delegate().isScope(annotationType);
    }

    @Override
    public boolean isNormalScope(Class<? extends Annotation> annotationType) {
        return delegate().isNormalScope(annotationType);
    }

    @Override
    public boolean isPassivatingScope(Class<? extends Annotation> annotationType) {
        return delegate().isPassivatingScope(annotationType);
    }

    @Override
    public boolean isQualifier(Class<? extends Annotation> annotationType) {
        return delegate().isQualifier(annotationType);
    }

    @Override
    public boolean isInterceptorBinding(Class<? extends Annotation> annotationType) {
        return delegate().isInterceptorBinding(annotationType);
    }

    @Override
    public boolean isStereotype(Class<? extends Annotation> annotationType) {
        return delegate().isStereotype(annotationType);
    }

    @Override
    public Set<Annotation> getInterceptorBindingDefinition(Class<? extends Annotation> bindingType) {
        return delegate().getInterceptorBindingDefinition(bindingType);
    }

    @Override
    public Set<Annotation> getStereotypeDefinition(Class<? extends Annotation> stereotype) {
        return delegate().getStereotypeDefinition(stereotype);
    }

    @Override
    public boolean areQualifiersEquivalent(Annotation qualifier1, Annotation qualifier2) {
        return delegate().areQualifiersEquivalent(qualifier1, qualifier2);
    }

    @Override
    public boolean areInterceptorBindingsEquivalent(Annotation interceptorBinding1, Annotation interceptorBinding2) {
        return delegate().areInterceptorBindingsEquivalent(interceptorBinding1, interceptorBinding2);
    }

    @Override
    public int getQualifierHashCode(Annotation qualifier) {
        return delegate().getQualifierHashCode(qualifier);
    }

    @Override
    public int getInterceptorBindingHashCode(Annotation interceptorBinding) {
        return delegate().getInterceptorBindingHashCode(interceptorBinding);
    }

    @Override
    public Context getContext(Class<? extends Annotation> scopeType) {
        return delegate().getContext(scopeType);
    }

    @Override
    public ELResolver getELResolver() {
        return delegate().getELResolver();
    }

    @Override
    public ExpressionFactory wrapExpressionFactory(ExpressionFactory expressionFactory) {
        return delegate().wrapExpressionFactory(expressionFactory);
    }

    @Override
    public <T> AnnotatedType<T> createAnnotatedType(Class<T> type) {
        return delegate().createAnnotatedType(type);
    }

    @Override
    public <T> AnnotatedType<T> getAnnotatedType(Class<T> type, String id) {
        return delegate().getAnnotatedType(type, id);
    }

    @Override
    public <T> Iterable<AnnotatedType<T>> getAnnotatedTypes(Class<T> type) {
        return delegate().getAnnotatedTypes(type);
    }

    @Override
    public <T> InjectionTarget<T> createInjectionTarget(AnnotatedType<T> type) {
        return delegate().createInjectionTarget(type);
    }

    @Override
    public <X> Producer<?> createProducer(AnnotatedField<? super X> field, Bean<X> declaringBean) {
        return delegate().createProducer(field, declaringBean);
    }

    @Override
    public <X> Producer<?> createProducer(AnnotatedMethod<? super X> method, Bean<X> declaringBean) {
        return delegate().createProducer(method, declaringBean);
    }

    @Override
    public <T> BeanAttributes<T> createBeanAttributes(AnnotatedType<T> type) {
        return delegate().createBeanAttributes(type);
    }

    @Override
    public BeanAttributes<?> createBeanAttributes(AnnotatedMember<?> type) {
        return delegate().createBeanAttributes(type);
    }

    @Override
    public <T> Bean<T> createBean(BeanAttributes<T> attributes, Class<T> beanClass, InjectionTarget<T> injectionTarget) {
        return delegate().createBean(attributes, beanClass, injectionTarget);
    }

    @Override
    public <T> Bean<T> createBean(BeanAttributes<T> attributes, Class<?> beanClass, Producer<T> producer) {
        return delegate().createBean(attributes, beanClass, producer);
    }

    @Override
    public InjectionPoint createInjectionPoint(AnnotatedField<?> field) {
        return null;
    }

    @Override
    public InjectionPoint createInjectionPoint(AnnotatedParameter<?> parameter) {
        return delegate().createInjectionPoint(parameter);
    }

    @Override
    public <T extends Extension> T getExtension(Class<T> extensionClass) {
        return delegate().getExtension(extensionClass);
    }

    // Serialization
    protected Object readResolve() {
        return Container.instance().activityManager(delegate().getId());
    }

    /**
    *
    * @param methodName
    * @throws IllegalStateException If the application initialization is not finished yet
    */
   private void checkApplicationInitializationFinished(String methodName) {

       if (ContainerState.VALIDATED.equals(delegate().getContainer().getState())) {
           return;
       }
       throw new IllegalStateException(METHOD_NOT_AVAILABLE_DURING_INITIALIZATION, methodName);
   }


}
