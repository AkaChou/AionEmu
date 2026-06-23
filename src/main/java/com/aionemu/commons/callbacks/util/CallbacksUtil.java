package com.aionemu.commons.callbacks.util;

import com.aionemu.commons.callbacks.Callback;
import com.aionemu.commons.callbacks.CallbackPriority;
import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.List;
import javassist.CtMethod;
import javassist.bytecode.AnnotationsAttribute;

/**
 * 回调工具类，提供回调相关的通用工具方法
 * Callback utility class that provides common utility methods for callbacks
 *
 * 包含注解检查、优先级计算和回调列表管理等功能
 * Includes functionality for annotation checking, priority calculation, and callback list management
 */
public class CallbacksUtil {

    /**
     * 检查方法是否包含指定的注解
     * Check if a method contains the specified annotation
     *
     * @param method 要检查的方法 / Method to check
     * @param annotation 要查找的注解类 / Annotation class to look for
     * @return 如果方法包含指定注解则返回true / Returns true if the method contains the specified annotation
     */
    public static boolean isAnnotationPresent(CtMethod method, Class<? extends Annotation> annotation) {
        Iterator i$ = method.getMethodInfo().getAttributes().iterator();

        while(i$.hasNext()) {
            Object o = i$.next();
            if (o instanceof AnnotationsAttribute) {
                AnnotationsAttribute attribute = (AnnotationsAttribute)o;
                if (attribute.getAnnotation(annotation.getName()) != null) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 获取回调的优先级值
     * Get the priority value of a callback
     *
     * @param callback 要获取优先级的回调对象 / Callback object to get priority from
     * @return 回调的优先级值，值越小优先级越高 / Priority value of the callback, lower value means higher priority
     */
    public static int getCallbackPriority(Callback callback) {
        if (callback instanceof CallbackPriority) {
            CallbackPriority instancePriority = (CallbackPriority)callback;
            return 0 - instancePriority.getPriority();
        } else {
            return 0;
        }
    }

    /**
     * 将回调插入到列表中的适当位置，保持优先级顺序
     * Insert a callback into the appropriate position in the list, maintaining priority order
     *
     * @param callback 要插入的回调对象 / Callback object to insert
     * @param list 回调列表 / List of callbacks
     */
    protected static void insertCallbackToList(Callback callback, List<Callback> list) {
        int callbackPriority = getCallbackPriority(callback);
        if (!list.isEmpty()) {
            int i = 0;

            for(int n = list.size(); i < n; ++i) {
                Callback c = (Callback)list.get(i);
                int cPrio = getCallbackPriority(c);
                if (callbackPriority < cPrio) {
                    list.add(i, callback);
                    return;
                }
            }
        }

        list.add(callback);
    }
}
