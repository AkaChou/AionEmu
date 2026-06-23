package com.aionemu.commons.callbacks.metadata;

import com.aionemu.commons.callbacks.Callback;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 全局回调注解，用于标记需要全局范围内进行回调处理的方法
 * Global callback annotation, used to mark methods that need callback processing in global scope
 *
 * 该注解只能应用于方法级别，用于定义全局性的回调处理机制
 * This annotation can only be applied at method level and is used to define global callback mechanisms
 *
 * 使用该注解的方法将在全局范围内被监听和处理，适用于需要统一处理的场景
 * Methods using this annotation will be monitored and processed globally,
 * suitable for scenarios requiring unified processing
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GlobalCallback {
    
    /**
     * 获取回调处理器类
     * Get the callback handler class
     *
     * @return 继承自Callback的回调处理器类
     *         The callback handler class that extends Callback
     */
    Class<? extends Callback> value();
}
