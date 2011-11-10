/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.weld.tck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;

/**
 * If test class property is set all test methods that don't belong to specified test class are excluded.
 * 
 * Maven Surefire plugin is not able to run single test that is located outside src/test.
 * 
 * @author Stuart Douglas
 */
public class SingleTestMethodInterceptor implements IMethodInterceptor {
    
	public static final String TEST_CLASS_PROPERTY = "tckTest";

    public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
        String test = System.getProperty(TEST_CLASS_PROPERTY);
        if (test == null || test.isEmpty()) {
        	
        	// Group test methods by class 
            Collections.sort(methods, new Comparator<IMethodInstance>() {
            	public int compare(IMethodInstance o1, IMethodInstance o2) {
                    int result= o1.getMethod().getTestClass().getName()
                      .compareTo(o2.getMethod().getTestClass().getName());
                    return result;
			}});
            return methods;
        }
        List<IMethodInstance> ret = new ArrayList<IMethodInstance>();
        if (test.contains(".")) {
            for (IMethodInstance method : methods) {
                if (method.getMethod().getTestClass().getName().equals(test)) {
                    ret.add(method);
                }
            }
        } else {
            for (IMethodInstance method : methods) {
                if (method.getMethod().getTestClass().getName().endsWith("." + test)) {
                    ret.add(method);
                }
            }
        }
        return ret;
    }
    
}
