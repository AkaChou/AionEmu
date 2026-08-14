package com.aionemu.commons.callbacks.enhancer;

import lombok.extern.slf4j.Slf4j;
import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.aionemu.commons.callbacks.Callback;
import com.aionemu.commons.callbacks.CallbackResult;
import com.aionemu.commons.callbacks.EnhancedObject;
import com.aionemu.commons.callbacks.metadata.ObjectCallback;
import com.aionemu.commons.callbacks.util.CallbacksUtil;
import com.aionemu.commons.callbacks.util.ObjectCallbackHelper;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.NotFoundException;

/**
 * 对象回调增强器，用于在运行时增强类的回调功能
 * Object Callback Enhancer for enhancing callback functionality of classes at runtime
 *
 * 该类通过字节码增强技术实现以下功能：
 * 1. 为目标类添加回调支持
 * 2. 注入回调前置和后置处理代码
 * 3. 管理回调方法的执行流程
 *
 * This class implements the following features through bytecode enhancement:
 * 1. Add callback support for target classes
 * 2. Inject callback pre/post processing code
 * 3. Manage callback method execution flow
 *
 * @author SoulKeeper
 */
@Slf4j
public class ObjectCallbackEnhancer extends CallbackClassFileTransformer {


    /**
     * 回调映射字段名称
     * Field name for callbacks map
     */
    public static final String FIELD_NAME_CALLBACKS = "$$$callbacks";

    /**
     * 回调锁字段名称
     * Field name for synchronizer
     */
    public static final String FIELD_NAME_CALLBACKS_LOCK = "$$$callbackLock";

    /**
     * 执行实际的类字节码转换与对象回调增强。
     * Perform the actual class bytecode transformation and object-callback enhancement.
     *
     * @param loader 类加载器 / class loader
     * @param clazzBytes 类字节码 / class bytecode
     * @return 增强后的字节码；无需增强时返回 null / enhanced bytecode, or null when no enhancement is needed
     * @throws Exception 增强失败时 / when enhancement fails
     */
    protected byte[] transformClass(ClassLoader loader, byte[] clazzBytes) throws Exception {
        ClassPool cp = new ClassPool();
        cp.appendClassPath(new LoaderClassPath(loader));
        CtClass clazz = cp.makeClass(new ByteArrayInputStream(clazzBytes));
        
        Set<CtMethod> methdosToEnhance = new HashSet<CtMethod>();
        
        for (CtMethod method : clazz.getDeclaredMethods()) {
            if (!isEnhanceable(method)) {
                continue;
            }
            
            methdosToEnhance.add(method);
        }
        
        if (!methdosToEnhance.isEmpty()) {
            CtClass eo = cp.get(EnhancedObject.class.getName());
            for (CtClass i : clazz.getInterfaces()) {
                if (i.equals(eo)) {
                    throw new RuntimeException("Class already implements EnhancedObject interface, WTF???");
                }
            }
            
            log.debug("Enhancing class: " + clazz.getName());
            writeEnhancedObjectImpl(clazz);
            
            for (CtMethod method : methdosToEnhance) {
                log.debug("Enhancing method: " + method.getLongName());
                enhanceMethod(method);
            }
            
            return clazz.toBytecode();
        } else {
            log.trace("Class " + clazz.getName() + " was not enhanced");
            return null;
        }
    }
    
    /**
     * 增强方法，写入前后置回调调用代码
     * Enhance a method by writing pre/post callback invocation code
     *
     * @param method 需要编辑的方法 / method that has to be edited
     * @throws CannotCompileException 代码编译失败时 / when code compilation fails
     * @throws NotFoundException 类型未找到时 / when a type is not found
     * @throws ClassNotFoundException 注解类无法加载时 / when an annotation class cannot be loaded
     */
    protected void enhanceMethod(CtMethod method) throws CannotCompileException, NotFoundException, ClassNotFoundException {
        ClassPool cp = method.getDeclaringClass().getClassPool();
        
        method.addLocalVariable("___cbr", cp.get(CallbackResult.class.getName()));
        
        CtClass listenerClazz = cp.get(((ObjectCallback) method.getAnnotation(ObjectCallback.class)).value().getName());
        
        String listenerFieldName = "$$$" + listenerClazz.getSimpleName();
        
        CtClass clazz = method.getDeclaringClass();
        try {
            clazz.getField(listenerFieldName);
        } catch (NotFoundException e) {
            clazz.addField(CtField.make("Class " + listenerFieldName + " = Class.forName(\"" + listenerClazz.getName() + "\");", clazz));
        }
        
        int paramLength = method.getParameterTypes().length;
        
        method.insertBefore(writeBeforeMethod(method, paramLength, listenerFieldName));
        method.insertAfter(writeAfterMethod(method, paramLength, listenerFieldName));
    }
    
    /**
     * 生成插入方法开头的前置回调代码
     * Generate code inserted at the beginning of the method for before-callbacks
     *
     * @param method 需要编辑的方法 / method that should be edited
     * @param paramLength 方法参数个数 / number of method parameters
     * @param listenerFieldName 监听器类字段名 / listener class field name used for the method
     * @return 插入方法前的代码 / code that will be inserted before the method body
     * @throws NotFoundException 类型未找到时 / when a type is not found
     * @throws CannotCompileException 代码生成失败时 / when code generation fails
     */
    protected String writeBeforeMethod(CtMethod method, int paramLength, String listenerFieldName) throws NotFoundException, CannotCompileException {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        
        sb.append(" ___cbr = ");
        sb.append(ObjectCallbackHelper.class.getName()).append(".beforeCall((");
        sb.append(EnhancedObject.class.getName());
        sb.append(")this, " + listenerFieldName + ", ");
        if (paramLength > 0) {
            sb.append("new Object[]{");
            for (int i = 1; i <= paramLength; i++) {
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
        
        sb.append("if(___cbr.isBlockingCaller()){");
        
        // 因 javassist 缺陷的假返回。 / Fake return due to javassist bug
        // $r is not available in "insertBefore"
        CtClass returnType = method.getReturnType();
        if (returnType.equals(CtClass.voidType)) {
            sb.append("return");
        } else if (returnType.equals(CtClass.booleanType)) {
            sb.append("return false");
        } else if (returnType.equals(CtClass.charType)) {
            sb.append("return 'a'");
        } else if (returnType.equals(CtClass.byteType) || returnType.equals(CtClass.shortType) || returnType.equals(CtClass.intType) || returnType.equals(CtClass.floatType)
                || returnType.equals(CtClass.longType)) {
            sb.append("return 0");
        }
        sb.append(";}}");
        
        return sb.toString();
    }
    
    /**
     * 生成插入方法结尾的后置回调代码
     * Generate code inserted after the method for after-callbacks
     *
     * @param method 需要编辑的方法 / method to edit
     * @param paramLength 方法参数个数 / number of method parameters
     * @param listenerFieldName 监听器类字段名 / method listener field name
     * @return 实际插入的代码 / actual code that should be inserted
     * @throws NotFoundException 类型未找到时 / when a type is not found
     */
    protected String writeAfterMethod(CtMethod method, int paramLength, String listenerFieldName) throws NotFoundException {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        
        // 规避 javassist 缺陷：insertBefore 中 $r 不可用 / workaround for javassist bug, $r is not available in "insertBefore"
        if (!method.getReturnType().equals(CtClass.voidType)) {
            sb.append("if(___cbr.isBlockingCaller()){");
            sb.append("$_ = ($r)($w)___cbr.getResult();");
            sb.append("}");
        }
        
        sb.append("___cbr = ").append(ObjectCallbackHelper.class.getName()).append(".afterCall((");
        sb.append(EnhancedObject.class.getName()).append(")this, " + listenerFieldName + ", ");
        if (paramLength > 0) {
            sb.append("new Object[]{");
            for (int i = 1; i <= paramLength; i++) {
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
        sb.append("if(___cbr.isBlockingCaller()){");
        if (method.getReturnType().equals(CtClass.voidType)) {
            sb.append("return;");
        } else {
            sb.append("return ($r)($w)___cbr.getResult();");
        }
        sb.append("}");
        sb.append("else {return $_;}");
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * 为实现 {@link EnhancedObject} 写入字段与方法
     * Write fields and methods that implement {@link EnhancedObject}
     *
     * @param clazz 需要编辑的类 / class to edit
     * @throws NotFoundException 类型未找到时 / when a type is not found
     * @throws CannotCompileException 代码编译失败时 / when code compilation fails
     */
    protected void writeEnhancedObjectImpl(CtClass clazz) throws NotFoundException, CannotCompileException {
        ClassPool cp = clazz.getClassPool();
        clazz.addInterface(cp.get(EnhancedObject.class.getName()));
        writeEnhancedOBjectFields(clazz);
        writeEnhancedObjectMethods(clazz);
    }
    
    /**
     * 为实现 {@link EnhancedObject} 添加字段
     * Add fields required by {@link EnhancedObject}
     *
     * @param clazz 需要添加字段的类 / class to add fields to
     * @throws CannotCompileException 字段生成失败时 / when field generation fails
     * @throws NotFoundException 类型未找到时 / when a type is not found
     */
    private void writeEnhancedOBjectFields(CtClass clazz) throws CannotCompileException, NotFoundException {
        ClassPool cp = clazz.getClassPool();
        
        // 添加保存回调的映射 / add map that holds callbacks
        CtField cbField = new CtField(cp.get(Map.class.getName()), FIELD_NAME_CALLBACKS, clazz);
        cbField.setModifiers(java.lang.reflect.Modifier.PRIVATE);
        clazz.addField(cbField, CtField.Initializer.byExpr("null;"));
        
        // 添加可重入读写锁 / add reetrantReadWriteLock
        CtField cblField = new CtField(cp.get(ReentrantReadWriteLock.class.getName()), FIELD_NAME_CALLBACKS_LOCK, clazz);
        cblField.setModifiers(java.lang.reflect.Modifier.PRIVATE);
        clazz.addField(cblField, CtField.Initializer.byExpr("new " + ReentrantReadWriteLock.class.getName() + "();"));
    }
    
    /**
     * 为实现 {@link EnhancedObject} 添加方法
     * Add methods required by {@link EnhancedObject}
     *
     * @param clazz 需要添加方法的类 / class to add methods to
     * @throws NotFoundException 类型未找到时 / when a type is not found
     * @throws CannotCompileException 方法生成失败时 / when method generation fails
     */
    private void writeEnhancedObjectMethods(CtClass clazz) throws NotFoundException, CannotCompileException {
        
        ClassPool cp = clazz.getClassPool();
        
        CtClass callbackClass = cp.get(Callback.class.getName());
        CtClass mapClass = cp.get(Map.class.getName());
        CtClass reentrantReadWriteLockClass = cp.get(ReentrantReadWriteLock.class.getName());
        
        CtMethod method = new CtMethod(CtClass.voidType, "addCallback", new CtClass[] {callbackClass}, clazz);
        method.setModifiers(java.lang.reflect.Modifier.PUBLIC);
        method.setBody("com.aionemu.commons.callbacks.util.ObjectCallbackHelper.addCallback($1, this);");
        clazz.addMethod(method);
        
        method = new CtMethod(CtClass.voidType, "removeCallback", new CtClass[] {callbackClass}, clazz);
        method.setModifiers(java.lang.reflect.Modifier.PUBLIC);
        method.setBody("com.aionemu.commons.callbacks.util.ObjectCallbackHelper.removeCallback($1, this);");
        clazz.addMethod(method);
        
        method = new CtMethod(mapClass, "getCallbacks", new CtClass[] {}, clazz);
        method.setModifiers(java.lang.reflect.Modifier.PUBLIC);
        method.setBody("return " + FIELD_NAME_CALLBACKS + ";");
        clazz.addMethod(method);
        
        method = new CtMethod(CtClass.voidType, "setCallbacks", new CtClass[] {mapClass}, clazz);
        method.setModifiers(java.lang.reflect.Modifier.PUBLIC);
        method.setBody("this." + FIELD_NAME_CALLBACKS + " = $1;");
        clazz.addMethod(method);
        
        method = new CtMethod(reentrantReadWriteLockClass, "getCallbackLock", new CtClass[] {}, clazz);
        method.setModifiers(java.lang.reflect.Modifier.PUBLIC);
        method.setBody("return " + FIELD_NAME_CALLBACKS_LOCK + ";");
        clazz.addMethod(method);
    }
    
    /**
     * 判断方法是否可增强：需带 {@link com.aionemu.commons.callbacks.metadata.ObjectCallback}，且非 native/abstract/static
     * Check whether a method is enhanceable: must have {@link com.aionemu.commons.callbacks.metadata.ObjectCallback} and not be native/abstract/static
     *
     * @param method 待检查方法 / Method to check
     * @return 可增强返回 true / True if enhanceable
     */
    protected boolean isEnhanceable(CtMethod method) {
        int modifiers = method.getModifiers();
        return !(Modifier.isAbstract(modifiers) || Modifier.isNative(modifiers) || Modifier.isStatic(modifiers)) && CallbacksUtil.isAnnotationPresent(method, ObjectCallback.class);
    }
}
