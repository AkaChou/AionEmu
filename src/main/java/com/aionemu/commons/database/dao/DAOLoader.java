package com.aionemu.commons.database.dao;

import java.lang.reflect.Modifier;

import com.aionemu.commons.scripting.classlistener.ClassListener;
import com.aionemu.commons.utils.ClassUtils;

/**
 * DAO加载器类
 * DAO Loader Class
 *
 * 这个工具类负责在脚本上下文初始化后加载所有DAO实现类。
 * This utility class is responsible for loading all DAO implementations after script context initialization.
 * DAO实现类必须满足以下条件：
 * DAO implementations must meet the following requirements:
 * - 必须是public类 / Must be public class
 * - 不能是抽象类或接口 / Cannot be abstract or interface
 * - 必须有默认的无参public构造函数 / Must have default no-arg public constructor
 * - 不能被@DisabledDAO注解标记 / Cannot be marked with @DisabledDAO annotation
 *
 * @author SoulKeeper
 * @author Aquanox
 */
public class DAOLoader implements ClassListener {

    /**
     * 在类加载后注册DAO实现
     * Register DAO implementations after class loading
     *
     * @param classes 需要处理的类数组 / Array of classes to process
     */
    @SuppressWarnings("unchecked")
    @Override
    public void postLoad(Class<?>[] classes) {
        // 注册DAO实现类 / Register DAO implementations
        for (Class<?> clazz : classes) {
            if (!isValidDAO(clazz)) {
                continue;
            }

            try {
                DAOManager.registerDAO((Class<? extends DAO>) clazz);
            } catch (Exception e) {
                throw new Error("Can't register DAO class", e);
            }
        }
    }

    /**
     * 在类卸载前注销DAO实现
     * Unregister DAO implementations before class unloading
     *
     * @param classes 需要处理的类数组 / Array of classes to process
     */
    @SuppressWarnings("unchecked")
    @Override
    public void preUnload(Class<?>[] classes) {
        // 注销DAO实现类 / Unregister DAO implementations
        for (Class<?> clazz : classes) {
            if (!isValidDAO(clazz)) {
                continue;
            }

            try {
                DAOManager.unregisterDAO((Class<? extends DAO>) clazz);
            } catch (Exception e) {
                throw new Error("Can't unregister DAO class", e);
            }
        }
    }

    /**
     * 检查类是否是有效的DAO实现
     * Check if the class is a valid DAO implementation
     *
     * @param clazz 要检查的类 / Class to check
     * @return 如果是有效的DAO实现返回true，否则返回false / Returns true if valid DAO implementation, false otherwise
     */
    public boolean isValidDAO(Class<?> clazz) {
        if (!ClassUtils.isSubclass(clazz, DAO.class)) {
            return false;
        }

        final int modifiers = clazz.getModifiers();

        if (Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers)) {
            return false;
        }

        if (!Modifier.isPublic(modifiers)) {
            return false;
        }

        if (clazz.isAnnotationPresent(DisabledDAO.class)) {
            return false;
        }

        return true;
    }
}
