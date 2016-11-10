package javax.enterprise.inject.spi;

import javax.enterprise.inject.spi.builder.AnnotatedTypeConfigurator;

/**
 *
 * @author Antoine Sabot-Durand
 * @since 2.0
 * @param <T> type for which the proxy is created
 */
public interface InterceptionFactory<T> {

    InterceptionFactory<T> ignoreFinalMethods();

    AnnotatedTypeConfigurator<T> configure();

    T createInterceptedInstance(T instance);

}
