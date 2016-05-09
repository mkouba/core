/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.weld.bean.proxy;

import static org.jboss.classfilewriter.util.DescriptorUtils.isPrimitive;
import static org.jboss.classfilewriter.util.DescriptorUtils.isWide;
import static org.jboss.classfilewriter.util.DescriptorUtils.makeDescriptor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;

import org.jboss.classfilewriter.AccessFlag;
import org.jboss.classfilewriter.ClassFile;
import org.jboss.classfilewriter.ClassMethod;
import org.jboss.classfilewriter.DuplicateMemberException;
import org.jboss.classfilewriter.code.BranchEnd;
import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.classfilewriter.util.Boxing;
import org.jboss.classfilewriter.util.DescriptorUtils;
import org.jboss.weld.annotated.enhanced.MethodSignature;
import org.jboss.weld.annotated.enhanced.jlr.MethodSignatureImpl;
import org.jboss.weld.bean.proxy.InterceptionDecorationContext.Stack;
import org.jboss.weld.exceptions.WeldException;
import org.jboss.weld.interceptor.proxy.LifecycleMixin;
import org.jboss.weld.interceptor.util.proxy.TargetInstanceProxy;
import org.jboss.weld.logging.BeanLogger;
import org.jboss.weld.security.GetDeclaredMethodsAction;
import org.jboss.weld.util.bytecode.BytecodeUtils;
import org.jboss.weld.util.bytecode.MethodInformation;
import org.jboss.weld.util.bytecode.RuntimeMethodInformation;
import org.jboss.weld.util.reflection.Reflections;

/**
 * Factory for producing subclasses that are used by the combined interceptors and decorators stack.
 *
 * @author Marius Bogoevici
 */
public class InterceptedSubclassFactory<T> extends ProxyFactory<T> {
    // Default proxy class name suffix
    public static final String PROXY_SUFFIX = "Subclass";

    private static final String SUPER_DELEGATE_SUFFIX = "$$super";

    private static final String COMBINED_INTERCEPTOR_AND_DECORATOR_STACK_METHOD_HANDLER_CLASS_NAME = CombinedInterceptorAndDecoratorStackMethodHandler.class.getName();
    private static final String[] INVOKE_METHOD_PARAMETERS = new String[] { makeDescriptor(Stack.class), LJAVA_LANG_OBJECT, LJAVA_LANG_REFLECT_METHOD, LJAVA_LANG_REFLECT_METHOD, "[" + LJAVA_LANG_OBJECT  };

    private final Set<MethodSignature> enhancedMethodSignatures;
    private final Set<MethodSignature> interceptedMethodSignatures;

    private final Class<?> proxiedBeanType;

    public InterceptedSubclassFactory(String contextId, Class<?> proxiedBeanType, Set<? extends Type> typeClosure, Bean<?> bean, Set<MethodSignature> enhancedMethodSignatures, Set<MethodSignature> interceptedMethodSignatures) {
        this(contextId, proxiedBeanType, typeClosure, getProxyName(contextId, proxiedBeanType, typeClosure, bean), bean, enhancedMethodSignatures, interceptedMethodSignatures);
    }

    /**
     * Creates a new proxy factory when the name of the proxy class is already
     * known, such as during de-serialization
     *
     * @param proxiedBeanType          the super-class for this proxy class
     * @param typeClosure              the bean types of the bean
     * @param enhancedMethodSignatures a restricted set of methods that need to be intercepted
     */

    public InterceptedSubclassFactory(String contextId, Class<?> proxiedBeanType, Set<? extends Type> typeClosure, String proxyName, Bean<?> bean, Set<MethodSignature> enhancedMethodSignatures, Set<MethodSignature> interceptedMethodSignatures) {
        super(contextId, proxiedBeanType, typeClosure, proxyName, bean, true);
        this.enhancedMethodSignatures = enhancedMethodSignatures;
        this.interceptedMethodSignatures = interceptedMethodSignatures;
        this.proxiedBeanType = proxiedBeanType;
    }

    @Override
    public void addInterfacesFromTypeClosure(Set<? extends Type> typeClosure, Class<?> proxiedBeanType) {
        for (Class<?> c : proxiedBeanType.getInterfaces()) {
            addInterface(c);
        }
    }

    /**
     * Returns a suffix to append to the name of the proxy class. The name
     * already consists of <class-name>_$$_Weld, to which the suffix is added.
     * This allows the creation of different types of proxies for the same class.
     *
     * @return a name suffix
     */
    protected String getProxyNameSuffix() {
        return PROXY_SUFFIX;
    }

    @Override
    protected void addMethods(ClassFile proxyClassType, ClassMethod staticConstructor) {
        // Add all class methods for interception
        addMethodsFromClass(proxyClassType, staticConstructor);

        // Add special proxy methods
        addSpecialMethods(proxyClassType, staticConstructor);

    }

    @Override
    protected void addMethodsFromClass(ClassFile proxyClassType, ClassMethod staticConstructor) {
        try {

            final Set<MethodSignature> finalMethods = new HashSet<MethodSignature>();
            final Set<MethodSignature> processedBridgeMethods = new HashSet<MethodSignature>();

            // Add all methods from the class hierarchy
            Class<?> cls = getBeanType();
            while (cls != null) {
                Set<MethodSignature> declaredBridgeMethods = new HashSet<MethodSignature>();
                for (Method method : AccessController.doPrivileged(new GetDeclaredMethodsAction(cls))) {

                    final MethodSignatureImpl methodSignature = new MethodSignatureImpl(method);

                    if (!Modifier.isFinal(method.getModifiers()) && !method.isBridge() && enhancedMethodSignatures.contains(methodSignature)
                            && !finalMethods.contains(methodSignature) && !processedBridgeMethods.contains(methodSignature)) {
                        try {
                            final MethodInformation methodInfo = new RuntimeMethodInformation(method);

                            if (interceptedMethodSignatures.contains(methodSignature)) {
                                // create delegate-to-super method
                                int modifiers = (method.getModifiers() | AccessFlag.SYNTHETIC | AccessFlag.PRIVATE) & ~AccessFlag.PUBLIC & ~AccessFlag.PROTECTED;
                                ClassMethod delegatingMethod = proxyClassType.addMethod(modifiers, method.getName() + SUPER_DELEGATE_SUFFIX, DescriptorUtils.makeDescriptor(method.getReturnType()),
                                        DescriptorUtils.parameterDescriptors(method.getParameterTypes()));
                                delegatingMethod.addCheckedExceptions((Class<? extends Exception>[]) method.getExceptionTypes());
                                createDelegateToSuper(delegatingMethod, methodInfo);

                                // this method is intercepted
                                // override a subclass method to delegate to method handler
                                ClassMethod classMethod = proxyClassType.addMethod(method);
                                addConstructedGuardToMethodBody(classMethod);
                                createForwardingMethodBody(classMethod, methodInfo, staticConstructor);
                                BeanLogger.LOG.addingMethodToProxy(method);
                            } else {
                                // this method is not intercepted
                                // we still need to override and push InterceptionDecorationContext stack to prevent full interception
                                ClassMethod classMethod = proxyClassType.addMethod(method);
                                new RunWithinInterceptionDecorationContextGenerator(classMethod, this) {

                                    @Override
                                    void doWork(CodeAttribute b, ClassMethod method) {
                                        // build the bytecode that invokes the super class method directly
                                        b.aload(0);
                                        // create the method invocation
                                        b.loadMethodParameters();
                                        b.invokespecial(methodInfo.getDeclaringClass(), methodInfo.getName(), methodInfo.getDescriptor());
                                        // leave the result on top of the stack
                                    }

                                    @Override
                                    void doReturn(CodeAttribute b, ClassMethod method) {
                                        // assumes doWork() result is on top of the stack
                                        b.returnInstruction();
                                    }
                                }.runStartIfNotOnTop();
                            }


                        } catch (DuplicateMemberException e) {
                            // do nothing. This will happen if superclass methods have
                            // been overridden
                        }
                    } else {
                        if (Modifier.isFinal(method.getModifiers())) {
                            finalMethods.add(methodSignature);
                        }
                        if (method.isBridge()) {
                            declaredBridgeMethods.add(methodSignature);
                        }
                    }
                }
                processedBridgeMethods.addAll(declaredBridgeMethods);
                cls = cls.getSuperclass();
            }
            for (Class<?> c : getAdditionalInterfaces()) {
                for (Method method : c.getMethods()) {
                    MethodSignature signature = new MethodSignatureImpl(method);
                    if (enhancedMethodSignatures.contains(signature) && !processedBridgeMethods.contains(signature)) {
                        final MethodSignatureImpl methodSignature = new MethodSignatureImpl(method);
                        try {
                            if(interceptedMethodSignatures.contains(methodSignature) && Reflections.isDefault(method)) {
                                // TODO
                                MethodInformation methodInfo = new RuntimeMethodInformation(method);
                                // create delegate-to-super method
                                int modifiers = (method.getModifiers() | AccessFlag.SYNTHETIC | AccessFlag.PRIVATE) & ~AccessFlag.PUBLIC & ~AccessFlag.PROTECTED;
                                ClassMethod delegatingMethod = proxyClassType.addMethod(modifiers, method.getName() + SUPER_DELEGATE_SUFFIX, DescriptorUtils.makeDescriptor(method.getReturnType()),
                                        DescriptorUtils.parameterDescriptors(method.getParameterTypes()));
                                delegatingMethod.addCheckedExceptions((Class<? extends Exception>[]) method.getExceptionTypes());
                                createDelegateToSuper(delegatingMethod, methodInfo, method.getDeclaringClass().getName());
                                //createDelegateToSuper(delegatingMethod, methodInfo);

                                // this method is intercepted
                                // override a subclass method to delegate to method handler
                                ClassMethod classMethod = proxyClassType.addMethod(method);
                                // addConstructedGuardToMethodBody(classMethod);
                                addConstructedGuardToMethodBody(classMethod, method.getDeclaringClass().getName());
                                createForwardingMethodBody(classMethod, methodInfo, staticConstructor);
                                BeanLogger.LOG.addingMethodToProxy(method);
                            } else {
                                MethodInformation methodInformation = new RuntimeMethodInformation(method);
                                final ClassMethod classMethod = proxyClassType.addMethod(method);
                                createSpecialMethodBody(classMethod, methodInformation, staticConstructor);
                                BeanLogger.LOG.addingMethodToProxy(method);
                            }

                        } catch (DuplicateMemberException e) {
                            // This will happen if an interface method has been implemented
                        }
                    }
                    if (method.isBridge()) {
                        processedBridgeMethods.add(signature);
                    }
                }
            }
        } catch (Exception e) {
            throw new WeldException(e);
        }
    }

    protected void createForwardingMethodBody(ClassMethod classMethod, MethodInformation method, ClassMethod staticConstructor) {
        createInterceptorBody(classMethod, method, true, staticConstructor);
    }

    /**
     * Creates the given method on the proxy class where the implementation
     * forwards the call directly to the method handler.
     * <p/>
     * the generated bytecode is equivalent to:
     * <p/>
     * return (RetType) methodHandler.invoke(this,param1,param2);
     *
     * @param methodInfo      any JLR method
     * @param delegateToSuper
     * @return the method byte code
     */

    protected void createInterceptorBody(ClassMethod method, MethodInformation methodInfo, boolean delegateToSuper, ClassMethod staticConstructor) {

        invokeMethodHandler(method, methodInfo, true, DEFAULT_METHOD_RESOLVER, delegateToSuper, staticConstructor);
    }

    private void createDelegateToSuper(ClassMethod classMethod, MethodInformation method) {
        createDelegateToSuper(classMethod, method, classMethod.getClassFile().getSuperclass());
    }

    private void createDelegateToSuper(ClassMethod classMethod, MethodInformation method, String className) {
        CodeAttribute b = classMethod.getCodeAttribute();
        // first generate the invokespecial call to the super class method
        b.aload(0);
        b.loadMethodParameters();
        b.invokespecial(className, method.getName(), method.getDescriptor());
        b.returnInstruction();
    }

    /**
     * calls methodHandler.invoke for a given method
     *
     * @param methodInfo             declaring class of the method
     * @param addReturnInstruction   set to true you want to return the result of
     * @param bytecodeMethodResolver The method resolver
     * @param addProceed
     */
    protected void invokeMethodHandler(ClassMethod method, MethodInformation methodInfo, boolean addReturnInstruction, BytecodeMethodResolver bytecodeMethodResolver, boolean addProceed, ClassMethod staticConstructor) {
        // now we need to build the bytecode. The order we do this in is as
        // follows:
        // load methodHandler
        // dup the methodhandler
        // invoke isDisabledHandler on the method handler to figure out of this is
        // a self invocation.

        // load this
        // load the method object
        // load the proceed method that invokes the superclass version of the
        // current method
        // create a new array the same size as the number of parameters
        // push our parameter values into the array
        // invokeinterface the invoke method
        // add checkcast to cast the result to the return type, or unbox if
        // primitive
        // add an appropriate return instruction
        final CodeAttribute b = method.getCodeAttribute();
        b.aload(0);
        getMethodHandlerField(method.getClassFile(), b);

        // this is a self invocation optimisation
        // test to see if this is a self invocation, and if so invokespecial the
        // superclass method directly
        if (addProceed) {
            b.dup();

            // get the Stack
            b.invokestatic(InterceptionDecorationContext.class.getName(), "getStack", "()" + DescriptorUtils.makeDescriptor(Stack.class));
            b.dupX1(); // Handler, Stack -> Stack, Handler, Stack
            b.invokevirtual(COMBINED_INTERCEPTOR_AND_DECORATOR_STACK_METHOD_HANDLER_CLASS_NAME, "isDisabledHandler", "(" + DescriptorUtils.makeDescriptor(Stack.class) + ")" + BytecodeUtils.BOOLEAN_CLASS_DESCRIPTOR);

            b.iconst(0);
            BranchEnd invokeSuperDirectly = b.ifIcmpeq();
            // now build the bytecode that invokes the super class method
            b.pop2(); // pop Stack and Handler
            b.aload(0);
            // create the method invocation
            b.loadMethodParameters();
            b.invokespecial(methodInfo.getDeclaringClass(), methodInfo.getName(), methodInfo.getDescriptor());
            b.returnInstruction();
            b.branchEnd(invokeSuperDirectly);
        } else {
            b.aconstNull();
        }

        b.aload(0);
        bytecodeMethodResolver.getDeclaredMethod(method, methodInfo.getDeclaringClass(), methodInfo.getName(), methodInfo.getParameterTypes(), staticConstructor);

        if (addProceed) {
            if (Modifier.isPrivate(method.getAccessFlags())) {
                // If the original method is private we can't use WeldSubclass.method$$super() as proceed
                bytecodeMethodResolver.getDeclaredMethod(method, methodInfo.getDeclaringClass(), methodInfo.getName(), methodInfo.getParameterTypes(),
                        staticConstructor);
            } else {
                bytecodeMethodResolver.getDeclaredMethod(method, method.getClassFile().getName(), methodInfo.getName() + SUPER_DELEGATE_SUFFIX,
                        methodInfo.getParameterTypes(), staticConstructor);
            }
        } else {
            b.aconstNull();
        }

        b.iconst(methodInfo.getParameterTypes().length);
        b.anewarray("java.lang.Object");

        int localVariableCount = 1;

        for (int i = 0; i < methodInfo.getParameterTypes().length; ++i) {
            String typeString = methodInfo.getParameterTypes()[i];
            b.dup(); // duplicate the array reference
            b.iconst(i);
            // load the parameter value
            BytecodeUtils.addLoadInstruction(b, typeString, localVariableCount);
            // box the parameter if necessary
            Boxing.boxIfNessesary(b, typeString);
            // and store it in the array
            b.aastore();
            if (isWide(typeString)) {
                localVariableCount = localVariableCount + 2;
            } else {
                localVariableCount++;
            }
        }
        // now we have all our arguments on the stack
        // lets invoke the method
        b.invokeinterface(StackAwareMethodHandler.class.getName(), "invoke", LJAVA_LANG_OBJECT, INVOKE_METHOD_PARAMETERS);
        if (addReturnInstruction) {
            // now we need to return the appropriate type
            if (methodInfo.getReturnType().equals(BytecodeUtils.VOID_CLASS_DESCRIPTOR)) {
                b.returnInstruction();
            } else if (isPrimitive(methodInfo.getReturnType())) {
                Boxing.unbox(b,method.getReturnType());
                b.returnInstruction();
            } else {
                String castType = methodInfo.getReturnType();
                if (!methodInfo.getReturnType().startsWith("[")) {
                    castType = methodInfo.getReturnType().substring(1).substring(0, methodInfo.getReturnType().length() - 2);
                }
                b.checkcast(castType);
                b.returnInstruction();
            }
        }
    }

    /**
     * Adds methods requiring special implementations rather than just
     * delegation.
     *
     * @param proxyClassType the Javassist class description for the proxy type
     */
    protected void addSpecialMethods(ClassFile proxyClassType, ClassMethod staticConstructor) {
        try {
            // Add special methods for interceptors
            for (Method method : LifecycleMixin.class.getMethods()) {
                BeanLogger.LOG.addingMethodToProxy(method);
                MethodInformation methodInfo = new RuntimeMethodInformation(method);
                createInterceptorBody(proxyClassType.addMethod(method), methodInfo, false, staticConstructor);
            }
            Method getInstanceMethod = TargetInstanceProxy.class.getMethod("getTargetInstance");
            Method getInstanceClassMethod = TargetInstanceProxy.class.getMethod("getTargetClass");
            generateGetTargetInstanceBody(proxyClassType.addMethod(getInstanceMethod));
            generateGetTargetClassBody(proxyClassType.addMethod(getInstanceClassMethod));

            Method setMethodHandlerMethod = ProxyObject.class.getMethod("setHandler", MethodHandler.class);
            generateSetMethodHandlerBody(proxyClassType.addMethod(setMethodHandlerMethod));

            Method getMethodHandlerMethod = ProxyObject.class.getMethod("getHandler");
            generateGetMethodHandlerBody(proxyClassType.addMethod(getMethodHandlerMethod));
       } catch (Exception e) {
            throw new WeldException(e);
        }
    }

    private static void generateGetTargetInstanceBody(ClassMethod method) {
        final CodeAttribute b = method.getCodeAttribute();
        b.aload(0);
        b.returnInstruction();
    }

    private static void generateGetTargetClassBody(ClassMethod method) {
        final CodeAttribute b = method.getCodeAttribute();
        BytecodeUtils.pushClassType(b, method.getClassFile().getSuperclass());
        b.returnInstruction();
    }

    @Override
    public Class<?> getBeanType() {
        return proxiedBeanType;
    }

    @Override
    protected boolean isCreatingProxy() {
        return false;
    }

    @Override
    protected Class<? extends MethodHandler> getMethodHandlerType() {
        return CombinedInterceptorAndDecoratorStackMethodHandler.class;
    }

}
