package com.aionemu.commons.objects.filter;

/**
 * AND条件对象过滤器
 * AND Condition Object Filter
 * 
 * 实现了多条件AND过滤机制，只有当所有子过滤器都接受对象时，才返回true
 * Implements a multi-condition AND filtering mechanism, returns true only when all sub-filters accept the object
 * 
 * @param <T> 需要过滤的对象类型 / The type of object to be filtered
 */
public class AndObjectFilter<T> implements ObjectFilter<T> {
    /**
     * 子过滤器数组
     * Array of sub-filters
     */
    private ObjectFilter<? super T>[] filters;

    /**
     * 构造函数，接收一个或多个子过滤器
     * Constructor that accepts one or more sub-filters
     * 
     * @param filters 子过滤器数组 / Array of sub-filters
     */
    public AndObjectFilter(ObjectFilter<? super T>... filters) {
        this.filters = filters;
    }

    /**
     * 判断对象是否满足所有子过滤器的条件
     * Determines if the object meets all sub-filters' criteria
     * 
     * @param object 待评估的对象 / The object to evaluate
     * @return true 如果对象满足所有子过滤器的条件，false 如果任一子过滤器不接受该对象
     *         true if the object meets all sub-filters' criteria, false if any sub-filter rejects it
     */
    public boolean acceptObject(T object) {
        for (ObjectFilter<? super T> filter : filters) {
            if (filter != null && !filter.acceptObject(object)) {
                return false;
            }
        }
        return true;
    }
}
