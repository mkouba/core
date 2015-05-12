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
package org.jboss.weld.bootstrap;

import java.lang.reflect.Member;
import java.util.Set;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.ProcessProducerField;
import javax.enterprise.inject.spi.ProcessProducerMethod;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedField;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedMethod;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.annotated.slim.SlimAnnotatedTypeStore;
import org.jboss.weld.bean.AbstractBean;
import org.jboss.weld.bean.AbstractClassBean;
import org.jboss.weld.bean.AbstractProducerBean;
import org.jboss.weld.bean.DecoratorImpl;
import org.jboss.weld.bean.DisposalMethod;
import org.jboss.weld.bean.InterceptorImpl;
import org.jboss.weld.bean.ManagedBean;
import org.jboss.weld.bean.NewBean;
import org.jboss.weld.bean.NewManagedBean;
import org.jboss.weld.bean.NewSessionBean;
import org.jboss.weld.bean.ProducerField;
import org.jboss.weld.bean.ProducerMethod;
import org.jboss.weld.bean.RIBean;
import org.jboss.weld.bean.SessionBean;
import org.jboss.weld.bean.attributes.BeanAttributesFactory;
import org.jboss.weld.bean.attributes.ExternalBeanAttributesFactory;
import org.jboss.weld.bean.builtin.AbstractBuiltInBean;
import org.jboss.weld.bean.builtin.ExtensionBean;
import org.jboss.weld.bean.builtin.ee.EEResourceProducerField;
import org.jboss.weld.bean.builtin.ee.StaticEEResourceProducerField;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.events.ContainerLifecycleEvents;
import org.jboss.weld.bootstrap.events.ProcessBeanAttributesImpl;
import org.jboss.weld.bootstrap.events.ProcessObserverMethodImpl;
import org.jboss.weld.ejb.InternalEjbDescriptor;
import org.jboss.weld.event.ObserverFactory;
import org.jboss.weld.event.ObserverMethodImpl;
import org.jboss.weld.logging.BeanLogger;
import org.jboss.weld.logging.BootstrapLogger;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.util.BeanMethods;
import org.jboss.weld.util.Observers;
import org.jboss.weld.util.collections.WeldCollections;
import org.jboss.weld.util.reflection.Reflections;

/**
 * @author Pete Muir
 * @author Ales Justin
 * @author Jozef Hartinger
 */
public class AbstractBeanDeployer<E extends BeanDeployerEnvironment> {

    private final BeanManagerImpl manager;
    private final ServiceRegistry services;
    private final E environment;
    protected final ContainerLifecycleEvents containerLifecycleEvents;
    protected final ClassTransformer classTransformer;
    protected final SlimAnnotatedTypeStore slimAnnotatedTypeStore;
    protected final SpecializationAndEnablementRegistry specializationAndEnablementRegistry;

    public AbstractBeanDeployer(BeanManagerImpl manager, ServiceRegistry services, E environment) {
        this.manager = manager;
        this.services = services;
        this.environment = environment;
        this.containerLifecycleEvents = manager.getServices().get(ContainerLifecycleEvents.class);
        this.classTransformer = services.get(ClassTransformer.class);
        this.slimAnnotatedTypeStore = services.get(SlimAnnotatedTypeStore.class);
        this.specializationAndEnablementRegistry = services.get(SpecializationAndEnablementRegistry.class);
    }

    protected BeanManagerImpl getManager() {
        return manager;
    }

    // interceptors, decorators and observers go first
    public AbstractBeanDeployer<E> deploySpecialized() {
        // ensure that all decorators are initialized before initializing
        // the rest of the beans
        for (DecoratorImpl<?> bean : getEnvironment().getDecorators()) {
            bean.initialize(getEnvironment());
            containerLifecycleEvents.fireProcessBean(getManager(), bean);
            manager.addDecorator(bean);
            BootstrapLogger.LOG.foundDecorator(bean);
        }
        for (InterceptorImpl<?> bean : getEnvironment().getInterceptors()) {
            bean.initialize(getEnvironment());
            containerLifecycleEvents.fireProcessBean(getManager(), bean);
            manager.addInterceptor(bean);
            BootstrapLogger.LOG.foundInterceptor(bean);
        }
        return this;
    }

    public AbstractBeanDeployer<E> initializeBeans() {
        for (RIBean<?> bean : getEnvironment().getBeans()) {
            bean.initialize(getEnvironment());
        }
        return this;
    }

    public AbstractBeanDeployer<E> fireBeanEvents() {
        for (RIBean<?> bean : getEnvironment().getBeans()) {
            fireBeanEvents(bean);
        }
        return this;
    }

    public void fireBeanEvents(RIBean<?> bean) {
        if (!(bean instanceof NewBean)) {
            if (bean instanceof AbstractProducerBean<?, ?, ?>) {
                containerLifecycleEvents.fireProcessProducer(manager, Reflections.<AbstractProducerBean<?, ?, Member>>cast(bean));
            } else if (bean instanceof AbstractClassBean<?>) {
                containerLifecycleEvents.fireProcessInjectionTarget(manager, (AbstractClassBean<?>) bean);
            }
            containerLifecycleEvents.fireProcessBean(getManager(), bean);
        }
    }

    public AbstractBeanDeployer<E> deployBeans() {
        manager.addBeans(getEnvironment().getBeans());
        return this;
    }

    public AbstractBeanDeployer<E> initializeObserverMethods() {
        for (ObserverInitializationContext<?, ?> observerInitializer : getEnvironment().getObservers()) {
            if (Observers.isObserverMethodEnabled(observerInitializer.getObserver(), manager)) {
                observerInitializer.initialize();
            }
        }
        return this;
    }

    public AbstractBeanDeployer<E> deployObserverMethods() {
        // TODO -- why do observers have to be the last?
        for (ObserverInitializationContext<?, ?> observerInitializer : getEnvironment().getObservers()) {
            if (Observers.isObserverMethodEnabled(observerInitializer.getObserver(), manager)) {
                BootstrapLogger.LOG.foundObserverMethod(observerInitializer.getObserver());
                ProcessObserverMethodImpl.fire(manager, observerInitializer.getObserver());
                manager.addObserver(observerInitializer.getObserver());
            }
        }
        return this;
    }

    /**
     * Creates the sub bean for an class (simple or enterprise) bean
     *
     * @param bean The class bean
     */
    protected <T> void createObserversProducersDisposers(AbstractClassBean<T> bean) {
        if (bean instanceof ManagedBean<?> || bean instanceof SessionBean<?>) {
            // disposal methods have to go first as we want them to be ready for resolution when initializing producer method/fields
            createDisposalMethods(bean, bean.getEnhancedAnnotated());
            createProducerMethods(bean, bean.getEnhancedAnnotated());
            createProducerFields(bean, bean.getEnhancedAnnotated());
            if (manager.isBeanEnabled(bean)) {
                createObserverMethods(bean, bean.getEnhancedAnnotated());
            }
        }
    }

    protected <X> DisposalMethod<X, ?> resolveDisposalMethod(BeanAttributes<?> attributes, AbstractClassBean<X> declaringBean) {
        Set<DisposalMethod<X, ?>> disposalBeans = environment.<X>resolveDisposalBeans(attributes.getTypes(), attributes.getQualifiers(), declaringBean);

        if (disposalBeans.size() == 1) {
            return disposalBeans.iterator().next();
        } else if (disposalBeans.size() > 1) {
            throw BeanLogger.LOG.multipleDisposalMethods(this, WeldCollections.toMultiRowString(disposalBeans));
        }
        return null;
    }

    protected <X> void createProducerMethods(AbstractClassBean<X> declaringBean, EnhancedAnnotatedType<X> type) {
        for (EnhancedAnnotatedMethod<?, ? super X> method : BeanMethods.filterOutBridgeMethods(type.getDeclaredEnhancedMethods(Produces.class))) {
            // create method for now
            // specialization and PBA processing is handled later
            createProducerMethod(declaringBean, method);
        }
    }

    protected <X> void createDisposalMethods(AbstractClassBean<X> declaringBean, EnhancedAnnotatedType<X> annotatedClass) {
        for (EnhancedAnnotatedMethod<?, ? super X> method : BeanMethods.filterOutBridgeMethods(annotatedClass
                .getDeclaredEnhancedMethodsWithAnnotatedParameters(Disposes.class))) {
            DisposalMethod<? super X, ?> disposalBean = DisposalMethod.of(manager, method, declaringBean);
            getEnvironment().addDisposesMethod(disposalBean);
        }
    }

    protected <X, T> void createProducerMethod(AbstractClassBean<X> declaringBean, EnhancedAnnotatedMethod<T, ? super X> annotatedMethod) {
        BeanAttributes<T> attributes = BeanAttributesFactory.forBean(annotatedMethod, getManager());
        DisposalMethod<X, ?> disposalMethod = resolveDisposalMethod(attributes, declaringBean);
        ProducerMethod<? super X, T> bean = ProducerMethod.of(attributes, annotatedMethod, declaringBean, disposalMethod, manager, services);
        containerLifecycleEvents.preloadProcessBeanAttributes(bean.getType());
        containerLifecycleEvents.preloadProcessBean(ProcessProducerMethod.class, annotatedMethod.getBaseType(), bean.getBeanClass());
        containerLifecycleEvents.preloadProcessProducer(bean.getBeanClass(), annotatedMethod.getBaseType());
        getEnvironment().addProducerMethod(bean);
    }

    protected <X, T> void createProducerField(AbstractClassBean<X> declaringBean, EnhancedAnnotatedField<T, ? super X> field) {
        BeanAttributes<T> attributes = BeanAttributesFactory.forBean(field, getManager());
        DisposalMethod<X, ?> disposalMethod = resolveDisposalMethod(attributes, declaringBean);
        ProducerField<X, T> bean;
        if (EEResourceProducerField.isEEResourceProducerField(manager, field)) {
            if (field.isStatic()) {
                bean = StaticEEResourceProducerField.of(attributes, field, declaringBean, disposalMethod, manager, services);
            } else {
                bean = EEResourceProducerField.of(attributes, field, declaringBean, disposalMethod, manager, services);
            }
        } else {
            bean = ProducerField.of(attributes, field, declaringBean, disposalMethod, manager, services);
        }
        containerLifecycleEvents.preloadProcessBeanAttributes(bean.getType());
        containerLifecycleEvents.preloadProcessBean(ProcessProducerField.class, field.getBaseType(), bean.getBeanClass());
        containerLifecycleEvents.preloadProcessProducer(bean.getBeanClass(), field.getBaseType());
        getEnvironment().addProducerField(bean);
    }

    protected <X> void createProducerFields(AbstractClassBean<X> declaringBean, EnhancedAnnotatedType<X> annotatedClass) {
        for (EnhancedAnnotatedField<?, ? super X> field : annotatedClass.getDeclaredEnhancedFields(Produces.class)) {
            createProducerField(declaringBean, field);
        }
    }

    protected <X> void createObserverMethods(AbstractClassBean<X> declaringBean, EnhancedAnnotatedType<? super X> annotatedClass) {
        for (EnhancedAnnotatedMethod<?, ? super X> method : BeanMethods.getObserverMethods(annotatedClass)) {
            createObserverMethod(declaringBean, method);
        }
    }

    protected <T, X> void createObserverMethod(AbstractClassBean<X> declaringBean, EnhancedAnnotatedMethod<T, ? super X> method) {
        ObserverMethodImpl<T, X> observer = ObserverFactory.create(method, declaringBean, manager);
        ObserverInitializationContext<T, ? super X> observerInitializer = ObserverInitializationContext.of(observer, method);
        containerLifecycleEvents.preloadProcessObserverMethod(observer.getObservedType(), declaringBean.getBeanClass());
        getEnvironment().addObserverMethod(observerInitializer);
    }

    protected <T> ManagedBean<T> createManagedBean(EnhancedAnnotatedType<T> weldClass) {
        BeanAttributes<T> attributes = BeanAttributesFactory.forBean(weldClass, getManager());
        ManagedBean<T> bean = ManagedBean.of(attributes, weldClass, manager);
        getEnvironment().addManagedBean(bean);
        return bean;
    }

    protected <T> void createNewManagedBean(EnhancedAnnotatedType<T> annotatedClass) {
        // TODO resolve existing beans first
        slimAnnotatedTypeStore.put(annotatedClass.slim());
        getEnvironment().addManagedBean(NewManagedBean.of(BeanAttributesFactory.forNewManagedBean(annotatedClass, manager), annotatedClass, manager));
    }

    protected <T> void createDecorator(EnhancedAnnotatedType<T> weldClass) {
        BeanAttributes<T> attributes = BeanAttributesFactory.forBean(weldClass, getManager());
        DecoratorImpl<T> bean = DecoratorImpl.of(attributes, weldClass, manager);
        getEnvironment().addDecorator(bean);
    }

    protected <T> void createInterceptor(EnhancedAnnotatedType<T> weldClass) {
        BeanAttributes<T> attributes = BeanAttributesFactory.forBean(weldClass, getManager());
        InterceptorImpl<T> bean = InterceptorImpl.of(attributes, weldClass, manager);
        getEnvironment().addInterceptor(bean);
    }

    protected <T> SessionBean<T> createSessionBean(InternalEjbDescriptor<T> descriptor) {
        EnhancedAnnotatedType<T> type = classTransformer.getEnhancedAnnotatedType(descriptor.getBeanClass(), getManager().getId());
        slimAnnotatedTypeStore.put(type.slim());
        return createSessionBean(descriptor, type);
    }
    protected <T> SessionBean<T> createSessionBean(InternalEjbDescriptor<T> descriptor, EnhancedAnnotatedType<T> weldClass) {
        // TODO Don't create enterprise bean if it has no local interfaces!
        BeanAttributes<T> attributes = BeanAttributesFactory.forSessionBean(weldClass, descriptor, getManager());
        SessionBean<T> bean = SessionBean.of(attributes, descriptor, manager, weldClass);
        getEnvironment().addSessionBean(bean);
        return bean;
    }

    protected <T> void createNewSessionBean(InternalEjbDescriptor<T> ejbDescriptor, BeanAttributes<?> originalAttributes, EnhancedAnnotatedType<?> type) {
        slimAnnotatedTypeStore.put(type.slim());
        BeanAttributes<T> attributes = Reflections.cast(BeanAttributesFactory.forNewSessionBean(originalAttributes, type.getJavaClass()));
        getEnvironment().addSessionBean(NewSessionBean.of(attributes, ejbDescriptor, manager));
    }

    public E getEnvironment() {
        return environment;
    }

    public void addBuiltInBean(AbstractBuiltInBean<?> bean) {
        getEnvironment().addBuiltInBean(bean);
    }

    public void addExtension(ExtensionBean bean) {
        getEnvironment().addExtension(bean);
    }

    protected <T, S> boolean fireProcessBeanAttributes(AbstractBean<T, S> bean) {
        if (!specializationAndEnablementRegistry.isCandidateForLifecycleEvent(bean)) {
            return false;
        }

        ProcessBeanAttributesImpl<T> event = containerLifecycleEvents.fireProcessBeanAttributes(getManager(), bean, bean.getAnnotated(), bean.getType());
        if (event == null) {
            return false;
        }
        if (event.isVeto()) {
            return true;
        }
        if (event.isDirty()) {
            bean.setAttributes(ExternalBeanAttributesFactory.<T>of(event.getBeanAttributesInternal(), manager));
            bean.checkSpecialization();
        }
        return false;
    }
}
