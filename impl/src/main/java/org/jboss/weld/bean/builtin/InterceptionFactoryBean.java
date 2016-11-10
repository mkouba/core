package org.jboss.weld.bean.builtin;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InterceptionFactory;

import org.jboss.weld.bootstrap.events.builder.AnnotatedTypeConfiguratorImpl;
import org.jboss.weld.injection.InterceptionFactoryImpl;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.util.collections.ImmutableSet;
import org.jboss.weld.util.reflection.Reflections;

/**
 *
 * @author Martin Kouba
 *
 * @param <T>
 */
public class InterceptionFactoryBean extends AbstractStaticallyDecorableBuiltInBean<InterceptionFactory<?>> {

    private static final Set<Type> TYPES = ImmutableSet.<Type> of(InterceptionFactory.class, Object.class);

    public InterceptionFactoryBean(BeanManagerImpl beanManager) {
        super(beanManager, Reflections.<Class<InterceptionFactory<?>>> cast(InterceptionFactory.class));
    }

    @Override
    protected InterceptionFactory<?> newInstance(InjectionPoint ip, CreationalContext<InterceptionFactory<?>> creationalContext) {
        AnnotatedParameter<?> annotatedParameter = (AnnotatedParameter<?>) ip.getAnnotated();
        ParameterizedType parameterizedType = (ParameterizedType) annotatedParameter.getBaseType();
        AnnotatedType<?> annotatedType = beanManager.createAnnotatedType(Reflections.getRawType(parameterizedType.getActualTypeArguments()[0]));
        return InterceptionFactoryImpl.of(beanManager, creationalContext, new AnnotatedTypeConfiguratorImpl<>(annotatedType));
    }

    @Override
    public Set<Type> getTypes() {
        return TYPES;
    }

    @Override
    public String toString() {
        return "Implicit Bean [" + InterceptionFactory.class.getName() + "] with qualifiers [@Default]";
    }

}
