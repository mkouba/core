/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
package org.jboss.weld.tests.contexts.activator.request;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import org.jboss.weld.context.activator.ActivateRequestScope;
import org.junit.Assert;

@Dependent
public class Foo {

    @Inject
    BeanManager beanManager;

    @Inject
    Bar bar;

    @ActivateRequestScope
    public int ping() {
        Assert.assertTrue("RequestScoped is not active!", beanManager.getContext(RequestScoped.class).isActive());
        return 1;
    }

    @ActivateRequestScope
    public int pingNested() {
        bar.increment();
        notInterceptedCall();
        return bar.increment();
    }

    @ActivateRequestScope
    public void pong() {
        beanManager.getContext(RequestScoped.class);
    }

    public void notInterceptedCall() {
        bar.increment();
    }


}
