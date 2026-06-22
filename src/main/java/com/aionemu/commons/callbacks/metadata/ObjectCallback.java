package com.aionemu.commons.callbacks.metadata;

import com.aionemu.commons.callbacks.Callback;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 对象级回调注解，用于标记需要在对象实例级别进行回调处理的方法
 * Object-level callback annotation, used to mark methods that need callback processing at object instance level
 *
 * 该注解只能应用于方法级别，用于定义特定对象实例的回调处理机制
 * This annotation can only be applied at method level and is used to define
 * callback mechanisms for specific object instances
 *
 * 使用该注解的方法将在对象实例范围内被监听和处理，适用于需要针对特定对象进行处理的场景
 * Methods using this annotation will be monitored and processed within the scope of object instances,
 * suitable for scenarios requiring processing for specific objects
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ObjectCallback {
    
    /**
     * 获取回调处理器类
     * Get the callback handler class
     *
     * @return 继承自Callback的回调处理器类
     *         The callback handler class that extends Callback
     */
    Class<? extends Callback> value();
}
