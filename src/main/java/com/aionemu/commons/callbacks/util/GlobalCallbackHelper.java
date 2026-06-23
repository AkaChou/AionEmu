package com.aionemu.commons.callbacks.util;

import com.aionemu.commons.callbacks.Callback;
import com.aionemu.commons.callbacks.CallbackResult;
import com.aionemu.commons.utils.ClassUtils;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 全局回调帮助类，提供全局级别的回调管理功能
 * Global callback helper class that provides global-level callback management
 *
 * 该类使用线程安全的CopyOnWriteArrayList存储全局回调
 * This class uses thread-safe CopyOnWriteArrayList to store global callbacks
 *
 * 支持添加、移除全局回调，以及在方法调用前后执行回调
 * Supports adding and removing global callbacks, and executing callbacks before and after method calls
 */
public class GlobalCallbackHelper {
    
    /**
     * 日志记录器
     * Logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(GlobalCallbackHelper.class);
    
    /**
     * 存储全局回调的线程安全列表
     * Thread-safe list for storing global callbacks
     */
    private static final CopyOnWriteArrayList<Callback> globalCallbacks = new CopyOnWriteArrayList();

    /**
     * 私有构造函数，防止实例化
     * Private constructor to prevent instantiation
     */
    private GlobalCallbackHelper() {
    }

    /**
     * 添加全局回调
     * Add a global callback
     *
     * @param callback 要添加的回调对象 / The callback to add
     */
    public static void addCallback(Callback<?> callback) {
        synchronized(GlobalCallbackHelper.class) {
            CallbacksUtil.insertCallbackToList(callback, globalCallbacks);
        }
    }

    /**
     * 移除全局回调
     * Remove a global callback
     *
     * @param callback 要移除的回调对象 / The callback to remove
     */
    public static void removeCallback(Callback<?> callback) {
        synchronized(GlobalCallbackHelper.class) {
            globalCallbacks.remove(callback);
        }
    }

    /**
     * 执行方法调用前的回调
     * Execute callbacks before method call
     *
     * @param obj 目标对象 / Target object
     * @param callbackClass 回调类型 / Callback class type
     * @param args 方法参数 / Method arguments
     * @return 回调结果 / Callback result
     */
    public static CallbackResult<?> beforeCall(Object obj, Class callbackClass, Object... args) {
        CallbackResult<?> cr = null;
        Iterator i$ = globalCallbacks.iterator();

        while(i$.hasNext()) {
            Callback cb = (Callback)i$.next();
            if (ClassUtils.isSubclass(cb.getBaseClass(), callbackClass)) {
                try {
                    cr = cb.beforeCall(obj, args);
                    if (cr.isBlockingCallbacks()) {
                        break;
                    }
                } catch (Exception var7) {
                    log.error("Exception in global callback", var7);
                }
            }
        }

        return cr == null ? CallbackResult.newContinue() : cr;
    }

    /**
     * 执行方法调用后的回调
     * Execute callbacks after method call
     *
     * @param obj 目标对象 / Target object
     * @param callbackClass 回调类型 / Callback class type
     * @param args 方法参数 / Method arguments
     * @param result 方法执行结果 / Method execution result
     * @return 回调结果 / Callback result
     */
    public static CallbackResult<?> afterCall(Object obj, Class callbackClass, Object[] args, Object result) {
        CallbackResult<?> cr = null;
        Iterator i$ = globalCallbacks.iterator();

        while(i$.hasNext()) {
            Callback cb = (Callback)i$.next();
            if (ClassUtils.isSubclass(cb.getBaseClass(), callbackClass)) {
                try {
                    cr = cb.afterCall(obj, args, result);
                    if (cr.isBlockingCallbacks()) {
                        break;
                    }
                } catch (Exception var8) {
                    log.error("Exception in global callback", var8);
                }
            }
        }

        return cr == null ? CallbackResult.newContinue() : cr;
    }
}
