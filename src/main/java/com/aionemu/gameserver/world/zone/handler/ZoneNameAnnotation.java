package com.aionemu.gameserver.world.zone.handler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 将区域脚本类绑定到一个或多个区域名称（空格分隔）。
 * Binds a zone-script class to one or more zone names (space-separated).
 *
 * @author MrPoke
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ZoneNameAnnotation {

	/**
	 * 区域名称列表，以空格分隔。
	 * Zone name list, separated by spaces.
	 *
	 * @return 区域名称字符串 / zone name string
	 */
	String value();
}
