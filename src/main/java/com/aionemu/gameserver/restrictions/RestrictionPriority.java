package com.aionemu.gameserver.restrictions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限制优先级；数值越大越先执行。
 * Restriction priority; higher values run first.
 *
 * @author NB4L1
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface RestrictionPriority {

	/**
	 * 默认优先级。
	 * Default priority.
	 */
	public static final double DEFAULT_PRIORITY = 0.0;

	/**
	 * 优先级值。
	 * Priority value.
	 *
	 * @return 优先级值 / priority
	 */
	double value() default DEFAULT_PRIORITY;
}
