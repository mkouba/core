package org.jboss.weld.bean.proxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jboss.weld.bean.proxy.InterceptionDecorationContext.Stack;
import org.jboss.weld.logging.BeanLogger;

public class InterceptedProxyMethodHandler extends CombinedInterceptorAndDecoratorStackMethodHandler {

    private final Object instance;

    public InterceptedProxyMethodHandler(Object instance) {
        this.instance = instance;
    }

    @Override
    public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
        if (BeanLogger.LOG.isTraceEnabled()) {
            BeanLogger.LOG.invokingMethodDirectly(thisMethod.toGenericString(), instance);
        }
        Object result = null;
        try {
            SecurityActions.ensureAccessible(thisMethod);
            result = thisMethod.invoke(instance, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
        return result;
    }

    @Override
    public Object invoke(Stack stack, Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
        return super.invoke(stack, instance, thisMethod, proceed, args);
    }

}
