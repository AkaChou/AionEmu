package com.aionemu.commons.callbacks;

/**
 * 回调系统的核心接口，定义了回调方法的基本契约
 * Core interface of the callback system that defines the basic contract for callback methods
 * 
 * @param <T> 被增强对象的类型 / Type of the enhanced object
 */
public interface Callback<T> {
    /**
     * 在目标方法调用前执行的回调方法
     * Callback method executed before the target method call
     *
     * @param var1 被增强的对象实例 / Enhanced object instance
     * @param var2 目标方法的参数数组 / Array of target method parameters
     * @return 回调结果，决定是否继续执行后续回调和目标方法 / Callback result that determines whether to continue with subsequent callbacks and target method
     */
    CallbackResult beforeCall(T var1, Object[] var2);

    /**
     * 在目标方法调用后执行的回调方法
     * Callback method executed after the target method call
     *
     * @param var1 被增强的对象实例 / Enhanced object instance
     * @param var2 目标方法的参数数组 / Array of target method parameters
     * @param var3 目标方法的返回结果 / Return result of the target method
     * @return 回调结果，可以修改原方法的返回值 / Callback result that can modify the original return value
     */
    CallbackResult afterCall(T var1, Object[] var2, Object var3);

    /**
     * 获取回调的基类类型
     * Get the base class type of the callback
     *
     * @return 回调接口的Class对象 / Class object of the callback interface
     */
    Class<? extends Callback> getBaseClass();
}
