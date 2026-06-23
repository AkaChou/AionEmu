package com.aionemu.commons.scripting.classlistener;

/**
 * 类生命周期监听器接口
 * Class lifecycle listener interface
 *
 * 该接口定义了类加载和卸载时的回调方法：
 * This interface defines callback methods for class loading and unloading:
 * - 类加载完成后的处理 (Processing after class loading)
 * - 类卸载前的处理 (Processing before class unloading)
 */
public interface ClassListener {
    /**
     * 在类加载完成后调用
     * Called after classes are loaded
     *
     * @param classes 已加载的类数组 / Array of loaded classes
     */
    void postLoad(Class<?>[] classes);

    /**
     * 在类卸载之前调用
     * Called before classes are unloaded
     *
     * @param classes 即将卸载的类数组 / Array of classes to be unloaded
     */
    void preUnload(Class<?>[] classes);
}
