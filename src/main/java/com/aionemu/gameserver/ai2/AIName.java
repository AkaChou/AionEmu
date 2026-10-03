package com.aionemu.gameserver.ai2;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记 AI 实现类的注册名称，供 {@link AI2Engine} 按名称查找并实例化。支持以逗号分隔声明多个别名。
 * Marks an AI implementation class with its registration name for lookup and instantiation by {@link AI2Engine}. Supports comma-separated aliases.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AIName {

	/**
	 * AI 注册名称（单个名称或以逗号分隔的别名列表）。
	 * Registered AI name(s) (single name or comma-separated list of aliases).
	 *
	 * @return AI 注册名称 / registered AI name(s)
	 */
	String value();
}
