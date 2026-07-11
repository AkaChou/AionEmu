package com.aionemu.gameserver.restrictions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记限制方法未实现，激活时跳过该方法。
 * Marks a restriction method as unimplemented so activation skips it.
 *
 * @author NB4L1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DisabledRestriction {

}
