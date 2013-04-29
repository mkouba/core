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
package org.jboss.weld.bean.builtin;

import static org.jboss.weld.logging.messages.BeanMessage.DESTROY_UNSUPPORTED;
import static org.jboss.weld.logging.messages.BeanMessage.PROXY_REQUIRED;
import static org.jboss.weld.logging.messages.BeanMessage.INSTANCE_ITERATOR_REMOVE_UNSUPPORTED;
import static org.jboss.weld.util.reflection.Reflections.cast;

import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Set;

import javax.enterprise.context.spi.AlterableContext;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.TypeLiteral;

import org.jboss.weld.bean.proxy.ProxyMethodHandler;
import org.jboss.weld.bean.proxy.ProxyObject;
import org.jboss.weld.context.WeldCreationalContext;
import org.jboss.weld.exceptions.InvalidObjectException;
import org.jboss.weld.exceptions.UnsupportedOperationException;
import org.jboss.weld.injection.CurrentInjectionPoint;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resolution.Resolvable;
import org.jboss.weld.resolution.ResolvableBuilder;
import org.jboss.weld.util.reflection.Formats;
import org.jboss.weld.util.reflection.Reflections;

import com.google.common.base.Preconditions;

/**
 * Helper implementation for Instance for getting instances
 *
 * @param <T>
 * @author Gavin King
 */
@edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SE_NO_SUITABLE_CONSTRUCTOR", "SE_BAD_FIELD" }, justification = "Uses SerializationProxy")
public class InstanceImpl<T> extends AbstractFacade<T, Instance<T>> implements Instance<T>, Serializable {

    private static final long serialVersionUID = -376721889693284887L;

    public static <I> Instance<I> of(InjectionPoint injectionPoint, CreationalContext<I> creationalContext,
            BeanManagerImpl beanManager) {
        return new InstanceImpl<I>(injectionPoint, creationalContext, beanManager);
    }

    private InstanceImpl(InjectionPoint injectionPoint, CreationalContext<? super T> creationalContext,
            BeanManagerImpl beanManager) {
        super(injectionPoint, creationalContext, beanManager);
    }

    public T get() {
        Resolvable resolvable = new ResolvableBuilder(getType(), getBeanManager()).addQualifiers(getQualifiers())
                .setDeclaringBean(getInjectionPoint().getBean()).create();
        Bean<?> bean = getBeanManager().getBean(resolvable);
        return getBeanInstance(bean);
    }

    private T getBeanInstance(Bean<?> bean) {
        // Generate a correct injection point for the bean, we do this by taking the original injection point and adjusting the
        // qualifiers and type
        InjectionPoint ip = new DynamicLookupInjectionPoint(getInjectionPoint(), getType(), getQualifiers());
        CurrentInjectionPoint currentInjectionPoint = getBeanManager().getServices().get(CurrentInjectionPoint.class);
        try {
            currentInjectionPoint.push(ip);
            return Reflections.<T> cast(getBeanManager().getReference(bean, getType(), getCreationalContext()));
        } finally {
            currentInjectionPoint.pop();
        }
    }

    /**
     * Gets a string representation
     *
     * @return A string representation
     */
    @Override
    public String toString() {
        return Formats.formatAnnotations(getQualifiers()) + " Instance<" + Formats.formatType(getType()) + ">";
    }

    private Set<Bean<?>> getBeans() {
        return getBeanManager().getBeans(getType(), getQualifiers());
    }

    public Iterator<T> iterator() {
        return new InstanceImplIterator(getBeans());
    }

    public boolean isAmbiguous() {
        return getBeans().size() > 1;
    }

    public boolean isUnsatisfied() {
        return getBeans().size() == 0;
    }

    public Instance<T> select(Annotation... qualifiers) {
        return selectInstance(this.getType(), qualifiers);
    }

    public <U extends T> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
        return selectInstance(subtype, qualifiers);
    }

    public <U extends T> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
        return selectInstance(subtype.getType(), qualifiers);
    }

    private <U extends T> Instance<U> selectInstance(Type subtype, Annotation[] newQualifiers) {
        InjectionPoint modifiedInjectionPoint = new FacadeInjectionPoint(getInjectionPoint(), subtype, getQualifiers(),
                newQualifiers);
        return new InstanceImpl<U>(modifiedInjectionPoint, getCreationalContext(), getBeanManager());
    }

    @Override
    public void destroy(T instance) {
        Preconditions.checkNotNull(instance);

        // check if this is a proxy of a normal-scoped bean
        if (instance instanceof ProxyObject) {
            ProxyObject proxy = (ProxyObject) instance;
            if (proxy.getHandler() instanceof ProxyMethodHandler) {
                ProxyMethodHandler handler = (ProxyMethodHandler) proxy.getHandler();
                Bean<?> bean = handler.getBean();
                Context context = getBeanManager().getContext(bean.getScope());
                if (context instanceof AlterableContext) {
                    AlterableContext alterableContext = (AlterableContext) context;
                    alterableContext.destroy(bean);
                    return;
                } else {
                    throw new UnsupportedOperationException(DESTROY_UNSUPPORTED, context);
                }
            }
        }

        // check if this is a dependent instance
        CreationalContext<? super T> ctx = getCreationalContext();
        if (ctx instanceof WeldCreationalContext<?>) {
            WeldCreationalContext<? super T> weldCtx = cast(ctx);
            weldCtx.destroyDependentInstance(instance);
        }
    }

    // Serialization

    private Object writeReplace() throws ObjectStreamException {
        return new SerializationProxy<T>(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException(PROXY_REQUIRED);
    }

    private static class SerializationProxy<T> extends AbstractFacadeSerializationProxy<T, Instance<T>> {

        private static final long serialVersionUID = 9181171328831559650L;

        public SerializationProxy(InstanceImpl<T> instance) {
            super(instance);
        }

        private Object readResolve() throws ObjectStreamException {
            return InstanceImpl.of(getInjectionPoint(), getCreationalContext(), getBeanManager());
        }

    }

    final class InstanceImplIterator implements Iterator<T> {

        private final Iterator<Bean<?>> delegate;

        private InstanceImplIterator(Set<Bean<?>> beans) {
            super();
            this.delegate = beans.iterator();
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public T next() {
            return getBeanInstance(delegate.next());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException(INSTANCE_ITERATOR_REMOVE_UNSUPPORTED);
        }

    }
}
