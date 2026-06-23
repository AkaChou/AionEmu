package com.aionemu.commons.callbacks.util;

import com.aionemu.commons.callbacks.Callback;
import com.aionemu.commons.callbacks.CallbackResult;
import com.aionemu.commons.callbacks.EnhancedObject;
import com.aionemu.commons.utils.GenericValidator;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 对象回调帮助类，提供对象级别的回调管理功能
 * Object callback helper class that provides object-level callback management
 *
 * 该类使用读写锁保证回调操作的线程安全性
 * This class uses read-write locks to ensure thread safety of callback operations
 *
 * 支持为增强对象添加、移除回调，以及在方法调用前后执行回调
 * Supports adding and removing callbacks for enhanced objects, and executing callbacks before and after method calls
 */
public class ObjectCallbackHelper {
    
    /**
     * 日志记录器
     * Logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(ObjectCallbackHelper.class);

    /**
     * 私有构造函数，防止实例化
     * Private constructor to prevent instantiation
     */
    private ObjectCallbackHelper() {
    }

    /**
     * 为增强对象添加回调
     * Add a callback to an enhanced object
     *
     * @param callback 要添加的回调对象 / The callback to add
     * @param object 目标增强对象 / The target enhanced object
     */
    public static void addCallback(Callback callback, EnhancedObject object) {
        try {
            object.getCallbackLock().writeLock().lock();
            Map<Class<? extends Callback>, List<Callback>> cbMap = object.getCallbacks();
            if (cbMap == null) {
                cbMap = Maps.newHashMap();
                object.setCallbacks(cbMap);
            }

            List<Callback> list = cbMap.get(callback.getBaseClass());
            if (list == null) {
                list = new CopyOnWriteArrayList();
                cbMap.put(callback.getBaseClass(), list);
            }

            CallbacksUtil.insertCallbackToList(callback, list);
        } finally {
            object.getCallbackLock().writeLock().unlock();
        }
    }

    /**
     * 从增强对象移除回调
     * Remove a callback from an enhanced object
     *
     * @param callback 要移除的回调对象 / The callback to remove
     * @param object 目标增强对象 / The target enhanced object
     */
    public static void removeCallback(Callback callback, EnhancedObject object) {
        try {
            object.getCallbackLock().writeLock().lock();
            Map<Class<? extends Callback>, List<Callback>> cbMap = object.getCallbacks();
            if (GenericValidator.isBlankOrNull(cbMap)) {
                return;
            }

            List<Callback> list = cbMap.get(callback.getBaseClass());
            if (list == null || !list.remove(callback)) {
                log.error("Attempt to remove callback that doesn't exists", new RuntimeException());
                return;
            }

            if (list.isEmpty()) {
                cbMap.remove(callback.getBaseClass());
            }

            if (cbMap.isEmpty()) {
                object.setCallbacks(null);
            }
        } finally {
            object.getCallbackLock().writeLock().unlock();
        }
    }

    /**
     * 执行方法调用前的回调
     * Execute callbacks before method call
     *
     * @param obj 目标增强对象 / Target enhanced object
     * @param callbackClass 回调类型 / Callback class type
     * @param args 方法参数 / Method arguments
     * @return 回调结果 / Callback result
     */
    public static CallbackResult<?> beforeCall(EnhancedObject obj, Class callbackClass, Object... args) {
        Map<Class<? extends Callback>, List<Callback>> cbMap = obj.getCallbacks();
        if (GenericValidator.isBlankOrNull(cbMap)) {
            return CallbackResult.newContinue();
        }

        CallbackResult<?> cr = null;
        List list = null;

        try {
            obj.getCallbackLock().readLock().lock();
            list = cbMap.get(callbackClass);
        } finally {
            obj.getCallbackLock().readLock().unlock();
        }

        if (GenericValidator.isBlankOrNull((Collection)list)) {
            return CallbackResult.newContinue();
        }

        Iterator i$ = list.iterator();
        while(i$.hasNext()) {
            Callback c = (Callback)i$.next();
            try {
                cr = c.beforeCall(obj, args);
                if (cr.isBlockingCallbacks()) {
                    break;
                }
            } catch (Exception var12) {
                log.error("Uncaught exception in callback", var12);
            }
        }

        return cr == null ? CallbackResult.newContinue() : cr;
    }

    /**
     * 执行方法调用后的回调
     * Execute callbacks after method call
     *
     * @param obj 目标增强对象 / Target enhanced object
     * @param callbackClass 回调类型 / Callback class type
     * @param args 方法参数 / Method arguments
     * @param result 方法执行结果 / Method execution result
     * @return 回调结果 / Callback result
     */
    public static CallbackResult<?> afterCall(EnhancedObject obj, Class callbackClass, Object[] args, Object result) {
        Map<Class<? extends Callback>, List<Callback>> cbMap = obj.getCallbacks();
        if (GenericValidator.isBlankOrNull(cbMap)) {
            return CallbackResult.newContinue();
        }

        CallbackResult<?> cr = null;
        List list = null;

        try {
            obj.getCallbackLock().readLock().lock();
            list = cbMap.get(callbackClass);
        } finally {
            obj.getCallbackLock().readLock().unlock();
        }

        if (GenericValidator.isBlankOrNull((Collection)list)) {
            return CallbackResult.newContinue();
        }

        Iterator i$ = list.iterator();
        while(i$.hasNext()) {
            Callback c = (Callback)i$.next();
            try {
                cr = c.afterCall(obj, args, result);
                if (cr.isBlockingCallbacks()) {
                    break;
                }
            } catch (Exception var13) {
                log.error("Uncaught exception in callback", var13);
            }
        }

        return cr == null ? CallbackResult.newContinue() : cr;
    }
}
