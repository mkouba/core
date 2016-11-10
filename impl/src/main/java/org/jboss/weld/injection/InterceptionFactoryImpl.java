package org.jboss.weld.injection;

import java.security.AccessController;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.InterceptionFactory;
import javax.enterprise.inject.spi.builder.AnnotatedTypeConfigurator;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.annotated.enhanced.MethodSignature;
import org.jboss.weld.annotated.enhanced.jlr.MethodSignatureImpl;
import org.jboss.weld.annotated.slim.unbacked.UnbackedAnnotatedType;
import org.jboss.weld.bean.proxy.InterceptedProxyFactory;
import org.jboss.weld.bean.proxy.InterceptedProxyMethodHandler;
import org.jboss.weld.bean.proxy.ProxyObject;
import org.jboss.weld.bootstrap.events.builder.AnnotatedTypeBuilderImpl;
import org.jboss.weld.bootstrap.events.builder.AnnotatedTypeConfiguratorImpl;
import org.jboss.weld.exceptions.UnproxyableResolutionException;
import org.jboss.weld.injection.producer.InterceptionModelInitializer;
import org.jboss.weld.interceptor.proxy.InterceptionContext;
import org.jboss.weld.interceptor.proxy.InterceptorMethodHandler;
import org.jboss.weld.interceptor.spi.model.InterceptionModel;
import org.jboss.weld.interceptor.spi.model.InterceptionType;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.util.Beans;
import org.jboss.weld.util.Proxies;

/**
 * TODO Right now, we create a new proxy class per each factory instance. But the proxy class could be shared under certain conditions.
 *
 *
 * @author Martin Kouba
 *
 * @param <T>
 */
public class InterceptionFactoryImpl<T> implements InterceptionFactory<T> {

    public static <F> InterceptionFactoryImpl<F> of(BeanManagerImpl beanManager, CreationalContext<?> creationalContext,
            AnnotatedTypeConfiguratorImpl<F> configurator) {
        return new InterceptionFactoryImpl<>(beanManager, creationalContext, configurator);
    }

    private static final AtomicLong INDEX = new AtomicLong();

    private final BeanManagerImpl beanManager;

    private final CreationalContext<?> creationalContext;

    private final AnnotatedTypeConfiguratorImpl<T> configurator;

    private boolean isUnproxyableValidationEnabled;

    private InterceptionFactoryImpl(BeanManagerImpl beanManager, CreationalContext<?> creationalContext, AnnotatedTypeConfiguratorImpl<T> configurator) {
        this.beanManager = beanManager;
        this.creationalContext = creationalContext;
        this.configurator = configurator;
        this.isUnproxyableValidationEnabled = true;
    }

    @Override
    public InterceptionFactory<T> ignoreFinalMethods() {
        // Note that final methods are always ignored
        isUnproxyableValidationEnabled = false;
        return this;
    }

    @Override
    public AnnotatedTypeConfigurator<T> configure() {
        return configurator;
    }

    @Override
    public T createInterceptedInstance(T instance) {

        if (isUnproxyableValidationEnabled) {
            UnproxyableResolutionException exception = Proxies.getUnproxyableTypeException(configurator.getAnnotated().getBaseType(), null,
                    beanManager.getServices(), !isUnproxyableValidationEnabled);
            if (exception != null) {
                throw exception;
            }
        }

        ClassTransformer classTransformer = beanManager.getServices().get(ClassTransformer.class);

        long idx = INDEX.incrementAndGet();
        String id = instance.getClass().getName() + "$$" + idx;
        UnbackedAnnotatedType<T> slimAnnotatedType = classTransformer.getUnbackedAnnotatedType(new AnnotatedTypeBuilderImpl<>(configurator).build(),
                beanManager.getId(), id);

        EnhancedAnnotatedType<T> enhancedAnnotatedType = classTransformer.getEnhancedAnnotatedType(slimAnnotatedType);

        // Init interception model
        new InterceptionModelInitializer<T>(beanManager, enhancedAnnotatedType, Beans.getBeanConstructor(enhancedAnnotatedType), null).init();
        InterceptionModel interceptionModel = beanManager.getInterceptorModelRegistry().get(slimAnnotatedType);

        boolean hasNonConstructorInterceptors = interceptionModel != null
                && (interceptionModel.hasExternalNonConstructorInterceptors() || interceptionModel.hasTargetClassInterceptors());

        if (!hasNonConstructorInterceptors) {
            return instance;
        }

        Set<MethodSignature> enhancedMethodSignatures = new HashSet<MethodSignature>();
        Set<MethodSignature> interceptedMethodSignatures = new HashSet<MethodSignature>();

        for (AnnotatedMethod<?> method : Beans.getInterceptableMethods(enhancedAnnotatedType)) {
            enhancedMethodSignatures.add(MethodSignatureImpl.of(method));
            if (!interceptionModel.getInterceptors(InterceptionType.AROUND_INVOKE, method.getJavaMember()).isEmpty()) {
                interceptedMethodSignatures.add(MethodSignatureImpl.of(method));
            }
        }

        InterceptedProxyFactory<T> proxyFactory = new InterceptedProxyFactory<>(beanManager.getContextId(), enhancedAnnotatedType.getJavaClass(),
                Collections.singleton(enhancedAnnotatedType.getJavaClass()), enhancedMethodSignatures, interceptedMethodSignatures, "" + idx);

        InterceptedProxyMethodHandler methodHandler = new InterceptedProxyMethodHandler(instance);
        methodHandler.setInterceptorMethodHandler(new InterceptorMethodHandler(
                InterceptionContext.forNonConstructorInterception(interceptionModel, creationalContext, beanManager, slimAnnotatedType)));

        T proxy = (System.getSecurityManager() == null) ? proxyFactory.run() : AccessController.doPrivileged(proxyFactory);
        ((ProxyObject) proxy).setHandler(methodHandler);

        return proxy;
    }

}
