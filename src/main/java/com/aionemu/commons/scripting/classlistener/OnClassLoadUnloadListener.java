package com.aionemu.commons.scripting.classlistener;

import com.aionemu.commons.scripting.metadata.OnClassLoad;
import com.aionemu.commons.scripting.metadata.OnClassUnload;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类加载/卸载注解处理监听器
 * Class load/unload annotation processing listener
 *
 * 该类负责处理类的加载和卸载事件，通过反射机制调用带有特定注解的静态方法：
 * This class handles class loading and unloading events by invoking annotated static methods through reflection:
 * - 处理@OnClassLoad注解的方法 (Process methods with @OnClassLoad annotation)
 * - 处理@OnClassUnload注解的方法 (Process methods with @OnClassUnload annotation)
 * - 自动管理方法的访问权限 (Automatically manage method accessibility)
 *
 * @author ATracer
 */
public class OnClassLoadUnloadListener implements ClassListener {

    /**
     * 日志记录器实例
     * Logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(OnClassLoadUnloadListener.class);

    /**
     * 处理类加载后的注解方法调用
     * Process annotated method calls after class loading
     *
     * @param classes 需要处理的类数组 / Array of classes to process
     */
    @Override
    public void postLoad(Class<?>[] classes) {
        for (Class<?> c : classes) {
            this.doMethodInvoke(c.getDeclaredMethods(), OnClassLoad.class);
        }
    }

    /**
     * 处理类卸载前的注解方法调用
     * Process annotated method calls before class unloading
     *
     * @param classes 需要处理的类数组 / Array of classes to process
     */
    @Override
    public void preUnload(Class<?>[] classes) {
        for (Class<?> c : classes) {
            this.doMethodInvoke(c.getDeclaredMethods(), OnClassUnload.class);
        }
    }

    /**
     * 执行带有指定注解的静态方法
     * Execute static methods with specified annotation
     *
     * @param methods 要检查的方法数组 / Array of methods to check
     * @param annotationClass 目标注解类 / Target annotation class
     */
    protected final void doMethodInvoke(Method[] methods, Class<? extends Annotation> annotationClass) {
        for (Method m : methods) {
            if (Modifier.isStatic(m.getModifiers())) {
                boolean accessible = m.isAccessible();
                m.setAccessible(true);
                
                if (m.getAnnotation(annotationClass) != null) {
                    try {
                        m.invoke(null);
                    } catch (IllegalAccessException e) {
                        log.error("Can't access method " + m.getName() + " of class " + m.getDeclaringClass().getName(), e);
                    } catch (InvocationTargetException e) {
                        log.error("Can't invoke method " + m.getName() + " of class " + m.getDeclaringClass().getName(), e);
                    }
                }
                
                m.setAccessible(accessible);
            }
        }
    }
}
