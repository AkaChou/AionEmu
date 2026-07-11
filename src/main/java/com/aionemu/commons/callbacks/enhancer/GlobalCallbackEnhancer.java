package com.aionemu.commons.callbacks.enhancer;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.callbacks.CallbackResult;
import com.aionemu.commons.callbacks.metadata.GlobalCallback;
import com.aionemu.commons.callbacks.util.CallbacksUtil;
import com.aionemu.commons.callbacks.util.GlobalCallbackHelper;
import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.NotFoundException;
/**
 * 全局回调增强器，用于处理@GlobalCallback 方法增强
 * Global callback enhancer for methods annotated with @GlobalCallback
 *
 * 该类继承自 CallbackClassFileTransformer，实现了全局回调的字节码增强功能
 * This class extends CallbackClassFileTransformer to implement bytecode enhancement for global callbacks
 *
 * 主要功能:
 * Main features:
 * 1. 识别和处理带有@GlobalCallback 注解的方法 / Identify and process methods with @GlobalCallback annotation
 * 2. 在方法前后插入回调逻辑 / Insert callback logic before and after methods
 * 3. 支持静态和非静态方法的回调处理 / Support callback handling for both static and non-static methods
 */
@Slf4j
public class GlobalCallbackEnhancer extends CallbackClassFileTransformer {
    

    /**
 * 执行类转换操作，增强带有@GlobalCallback 注解的方法
     * Perform class transformation to enhance methods with @GlobalCallback annotation
     *
     * Class loader
     * Class bytecode
     *
     * @param loader @return 转换后的字节码 / Transformed bytecode
     * @param clazzBytes @throws Exception 转换过程中的异常 / Exception during transformation
     */
    protected byte[] transformClass(ClassLoader loader, byte[] clazzBytes) throws Exception {
        ClassPool cp = new ClassPool();
        cp.appendClassPath(new LoaderClassPath(loader));
        CtClass clazz = cp.makeClass(new ByteArrayInputStream(clazzBytes));
        Set<CtMethod> methodsToEnhance = new HashSet<>();
        
        for (CtMethod method : clazz.getDeclaredMethods()) {
            if (isEnhanceable(method)) {
                methodsToEnhance.add(method);
            }
        }
        
        if (methodsToEnhance.isEmpty()) {
            log.trace("Class " + clazz.getName() + " was not enhanced");
            return null;
        }
        
        log.debug("Enhancing class: " + clazz.getName());
        for (CtMethod method : methodsToEnhance) {
            log.debug("Enhancing method: " + method.getLongName());
            enhanceMethod(method);
        }
        
        return clazz.toBytecode();
    }

    /**
     * 增强单个方法，添加回调相关的代码
     * Enhance a single method by adding callback-related code
     *
     * @param method 要增强的方法 / Method to enhance
     * Compilation exception
     * Class not found exception。 / Class not found exception.
     * Class loading exception。 / Class loading exception.
     */
    protected void enhanceMethod(CtMethod method) throws CannotCompileException, NotFoundException, ClassNotFoundException {
        ClassPool cp = method.getDeclaringClass().getClassPool();
        method.addLocalVariable("___globalCallbackResult", cp.get(CallbackResult.class.getName()));
        CtClass listenerClazz = cp.get(((GlobalCallback)method.getAnnotation(GlobalCallback.class)).value().getName());
        boolean isStatic = Modifier.isStatic(method.getModifiers());
        String listenerFieldName = "$$$" + (isStatic ? "Static" : "") + listenerClazz.getSimpleName();
        CtClass clazz = method.getDeclaringClass();

        try {
            clazz.getField(listenerFieldName);
        } catch (NotFoundException var8) {
            clazz.addField(CtField.make((isStatic ? "static " : "") + "Class " + listenerFieldName + " = Class.forName(\"" + listenerClazz.getName() + "\");", clazz));
        }

        int paramLength = method.getParameterTypes().length;
        method.insertBefore(writeBeforeMethod(method, paramLength, listenerFieldName));
        method.insertAfter(writeAfterMethod(method, paramLength, listenerFieldName));
    }

    /**
     * 生成方法执行前的回调代码
     * Generate callback code for before method execution
     *
     * Target method
     * Parameter length
     *
     * @param listenerFieldName 监听器字段名 / Listener field name
     * @param paramLength @return 生成的代码 / Generated code
     * @param listenerFieldName @throws NotFoundException 类未找到异常 / Class not found exception
     */
    protected String writeBeforeMethod(CtMethod method, int paramLength, String listenerFieldName) throws NotFoundException {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append(" ___globalCallbackResult = ");
        sb.append(GlobalCallbackHelper.class.getName()).append(".beforeCall(");
        if (Modifier.isStatic(method.getModifiers())) {
            sb.append(method.getDeclaringClass().getName()).append(".class, " + listenerFieldName);
            sb.append(", ");
        } else {
            sb.append("this, " + listenerFieldName);
            sb.append(", ");
        }

        if (paramLength > 0) {
            sb.append("new Object[]{");
            for (int i = 1; i <= paramLength; ++i) {
                sb.append("($w)$").append(i);
                if (i < paramLength) {
                    sb.append(',');
                }
            }
            sb.append("}");
        } else {
            sb.append("null");
        }

        sb.append(");");
        sb.append("if(___globalCallbackResult.isBlockingCaller()){");
        CtClass returnType = method.getReturnType();
        if (returnType.equals(CtClass.voidType)) {
            sb.append("return");
        } else if (returnType.equals(CtClass.booleanType)) {
            sb.append("return false");
        } else if (returnType.equals(CtClass.charType)) {
            sb.append("return 'a'");
        } else if (returnType.equals(CtClass.byteType) || returnType.equals(CtClass.shortType) 
                || returnType.equals(CtClass.intType) || returnType.equals(CtClass.floatType) 
                || returnType.equals(CtClass.longType)) {
            sb.append("return 0");
        }
        sb.append(";}}");
        return sb.toString();
    }

    /**
     * 生成方法执行后的回调代码
     * Generate callback code for after method execution
     *
     * Target method
     * Parameter length
     *
     * @param listenerFieldName 监听器字段名 / Listener field name
     * @param paramLength @return 生成的代码 / Generated code
     * @param listenerFieldName @throws NotFoundException 类未找到异常 / Class not found exception
     */
    protected String writeAfterMethod(CtMethod method, int paramLength, String listenerFieldName) throws NotFoundException {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        if (!method.getReturnType().equals(CtClass.voidType)) {
            sb.append("if(___globalCallbackResult.isBlockingCaller()){");
            sb.append("$_ = ($r)($w)___globalCallbackResult.getResult();");
            sb.append("}");
        }

        sb.append("___globalCallbackResult = ").append(GlobalCallbackHelper.class.getName()).append(".afterCall(");
        if (Modifier.isStatic(method.getModifiers())) {
            sb.append(method.getDeclaringClass().getName()).append(".class, " + listenerFieldName);
            sb.append(", ");
        } else {
            sb.append("this, ");
            sb.append(listenerFieldName + ", ");
        }

        if (paramLength > 0) {
            sb.append("new Object[]{");
            for (int i = 1; i <= paramLength; ++i) {
                sb.append("($w)$").append(i);
                if (i < paramLength) {
                    sb.append(',');
                }
            }
            sb.append("}");
        } else {
            sb.append("null");
        }

        sb.append(", ($w)$_);");
        sb.append("if(___globalCallbackResult.isBlockingCaller()){");
        if (method.getReturnType().equals(CtClass.voidType)) {
            sb.append("return;");
        } else {
            sb.append("return ($r)($w)___globalCallbackResult.getResult();");
        }
        sb.append("}");
        sb.append("else {return $_;}");
        sb.append("}");
        return sb.toString();
    }

    /**
     * 判断方法是否可以被增强
     * Determine if a method can be enhanced
     *
     * @param method 要检查的方法 / Method to check
     * @return 是否可以增强 / Whether can be enhanced
     */
    protected boolean isEnhanceable(CtMethod method) {
        int modifiers = method.getModifiers();
        return !Modifier.isAbstract(modifiers) && !Modifier.isNative(modifiers) 
                && CallbacksUtil.isAnnotationPresent(method, GlobalCallback.class);
    }
}
