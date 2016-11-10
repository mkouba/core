package org.jboss.weld.bean.proxy;

import static org.jboss.classfilewriter.util.DescriptorUtils.isPrimitive;
import static org.jboss.classfilewriter.util.DescriptorUtils.isWide;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.util.HashSet;
import java.util.Set;

import org.jboss.classfilewriter.ClassFile;
import org.jboss.classfilewriter.ClassMethod;
import org.jboss.classfilewriter.DuplicateMemberException;
import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.classfilewriter.util.Boxing;
import org.jboss.classfilewriter.util.DescriptorUtils;
import org.jboss.weld.annotated.enhanced.MethodSignature;
import org.jboss.weld.annotated.enhanced.jlr.MethodSignatureImpl;
import org.jboss.weld.bean.proxy.InterceptionDecorationContext.Stack;
import org.jboss.weld.exceptions.WeldException;
import org.jboss.weld.logging.BeanLogger;
import org.jboss.weld.security.GetDeclaredMethodsAction;
import org.jboss.weld.util.bytecode.BytecodeUtils;
import org.jboss.weld.util.bytecode.MethodInformation;
import org.jboss.weld.util.bytecode.RuntimeMethodInformation;

/**
 *
 * @author Martin Kouba
 *
 * @param <T>
 */
public class InterceptedProxyFactory<T> extends ProxyFactory<T> {

    public static final String PROXY_SUFFIX = "InterceptedProxy";

    private static final String INVOKE = "invoke";

    private static final String JAVA_LANG_OBJECT = "java.lang.Object";

    private final Set<MethodSignature> enhancedMethodSignatures;

    private final Set<MethodSignature> interceptedMethodSignatures;

    private final String suffix;

    public InterceptedProxyFactory(String contextId, Class<?> proxiedBeanType, Set<? extends Type> typeClosure, Set<MethodSignature> enhancedMethodSignatures,
            Set<MethodSignature> interceptedMethodSignatures, String suffix) {
        super(contextId, proxiedBeanType, typeClosure, null);
        this.enhancedMethodSignatures = enhancedMethodSignatures;
        this.interceptedMethodSignatures = interceptedMethodSignatures;
        this.suffix = suffix;
    }

    protected String getProxyNameSuffix() {
        return PROXY_SUFFIX + suffix;
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

                    if (isMethodAccepted(method) && enhancedMethodSignatures.contains(methodSignature) && !finalMethods.contains(methodSignature)
                            && !processedBridgeMethods.contains(methodSignature)) {
                        try {
                            final MethodInformation methodInfo = new RuntimeMethodInformation(method);
                            ClassMethod classMethod = proxyClassType.addMethod(method);

                            if (interceptedMethodSignatures.contains(methodSignature)) {
                                // this method is intercepted

                                final CodeAttribute b = classMethod.getCodeAttribute();

                                b.aload(0);
                                getMethodHandlerField(classMethod.getClassFile(), b);

                                // get the Stack
                                b.invokestatic(InterceptionDecorationContext.class.getName(), "getStack", "()" + DescriptorUtils.makeDescriptor(Stack.class));

                                b.aload(0);
                                DEFAULT_METHOD_RESOLVER.getDeclaredMethod(classMethod, methodInfo.getDeclaringClass(), method.getName(),
                                        methodInfo.getParameterTypes(), staticConstructor);
                                b.dup();
                                // Params
                                b.iconst(method.getParameterTypes().length);
                                b.anewarray(JAVA_LANG_OBJECT);
                                int localVariableCount = 1;
                                for (int i = 0; i < method.getParameterTypes().length; ++i) {
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

                                b.invokeinterface(StackAwareMethodHandler.class.getName(), INVOKE, LJAVA_LANG_OBJECT,
                                        InterceptedSubclassFactory.INVOKE_METHOD_PARAMETERS);

                                if (methodInfo.getReturnType().equals(BytecodeUtils.VOID_CLASS_DESCRIPTOR)) {
                                    b.returnInstruction();
                                } else if (isPrimitive(methodInfo.getReturnType())) {
                                    Boxing.unbox(b, classMethod.getReturnType());
                                    b.returnInstruction();
                                } else {
                                    b.checkcast(BytecodeUtils.getName(methodInfo.getReturnType()));
                                    b.returnInstruction();
                                }
                                BeanLogger.LOG.addingMethodToProxy(method);
                            } else {

                                final CodeAttribute b = classMethod.getCodeAttribute();

                                b.aload(0);
                                getMethodHandlerField(classMethod.getClassFile(), b);

                                b.aload(0);
                                DEFAULT_METHOD_RESOLVER.getDeclaredMethod(classMethod, methodInfo.getDeclaringClass(), method.getName(),
                                        methodInfo.getParameterTypes(), staticConstructor);
                                b.aconstNull();

                                b.iconst(method.getParameterTypes().length);
                                b.anewarray(JAVA_LANG_OBJECT);

                                int localVariableCount = 1;

                                for (int i = 0; i < method.getParameterTypes().length; ++i) {
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

                                b.invokeinterface(MethodHandler.class.getName(), INVOKE, LJAVA_LANG_OBJECT,
                                        new String[] { LJAVA_LANG_OBJECT, LJAVA_LANG_REFLECT_METHOD, LJAVA_LANG_REFLECT_METHOD, "[" + LJAVA_LANG_OBJECT });

                                if (methodInfo.getReturnType().equals(BytecodeUtils.VOID_CLASS_DESCRIPTOR)) {
                                    b.returnInstruction();
                                } else if (isPrimitive(methodInfo.getReturnType())) {
                                    Boxing.unbox(b, classMethod.getReturnType());
                                    b.returnInstruction();
                                } else {
                                    b.checkcast(BytecodeUtils.getName(methodInfo.getReturnType()));
                                    b.returnInstruction();
                                }
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
        } catch (Exception e) {
            // TODO remove print stack
            e.printStackTrace();
            throw new WeldException(e);
        }
    }

    @Override
    protected boolean isMethodAccepted(Method method) {
        return super.isMethodAccepted(method) && CommonProxiedMethodFilters.NON_PRIVATE.accept(method) && !method.isBridge();
    }

}
