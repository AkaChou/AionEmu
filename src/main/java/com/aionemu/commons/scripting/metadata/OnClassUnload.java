package com.aionemu.commons.scripting.metadata;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 类卸载注解，用于标记在类卸载时需要执行的方法
 * Class unloading annotation, used to mark methods that need to be executed when a class is unloaded
 *
 * 该注解只能应用于方法级别，在类卸载时由类加载监听器调用被注解的方法
 * This annotation can only be applied at method level and the annotated method
 * will be called by class load listener when the class is unloaded
 *
 * 使用要求:
 * Usage requirements:
 * 1. 只能注解静态方法
 *    Can only annotate static methods
 * 2. 方法不能有参数
 *    Method must have no parameters
 * 3. 方法必须是public或protected
 *    Method must be public or protected
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OnClassUnload {
}
