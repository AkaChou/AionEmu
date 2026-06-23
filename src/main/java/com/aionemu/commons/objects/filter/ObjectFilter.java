package com.aionemu.commons.objects.filter;

/**
 * 对象过滤器接口
 * Object Filter Interface
 * 
 * 定义了一个通用的对象过滤机制，用于根据特定条件过滤对象
 * Defines a generic object filtering mechanism for filtering objects based on specific conditions
 * 
 * @param <T> 需要过滤的对象类型 / The type of object to be filtered
 */
public interface ObjectFilter<T> {
    /**
     * 判断对象是否满足过滤条件
     * Determines if the object meets the filter criteria
     * 
     * @param object 待评估的对象 / The object to evaluate
     * @return true 如果对象满足过滤条件，false 否则
     *         true if the object meets the filter criteria, false otherwise
     */
    boolean acceptObject(T object);
}
