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
package org.jboss.weld.tests.bootstrap.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.BeanArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.weld.bootstrap.ConcurrentValidator;
import org.jboss.weld.bootstrap.ContainerLifecycleEventPreloader;
import org.jboss.weld.bootstrap.Validator;
import org.jboss.weld.executor.CachedThreadPoolExecutorServices;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.manager.api.ExecutorServices;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class CachedThreadPoolBootstrapConfigurationTest {

    @Inject
    private BeanManagerImpl manager;

    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(BeanArchive.class).addAsResource(new StringAsset("threadPoolType=CACHED\nthreadPoolKeepAliveTimeSeconds=0"),
                "org.jboss.weld.bootstrap.properties");
    }

    @Test
    public void testServices() throws Exception {
        assertTrue(manager.getServices().get(Validator.class) instanceof ConcurrentValidator);
        assertNotNull(manager.getServices().get(ContainerLifecycleEventPreloader.class));
        assertTrue(manager.getServices().get(ExecutorServices.class) instanceof CachedThreadPoolExecutorServices);
        Thread.sleep(100l);
        CachedThreadPoolExecutorServices executorServices = (CachedThreadPoolExecutorServices) manager.getServices().get(ExecutorServices.class);    
        assertEquals(0, executorServices.getPoolSize());
    }
}
