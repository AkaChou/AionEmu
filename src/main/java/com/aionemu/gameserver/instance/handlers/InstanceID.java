package com.aionemu.gameserver.instance.handlers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记副本处理器对应的世界地图 ID。
 * Marks the world-map id handled by an instance-handler class.
 *
 * @author ATracer
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface InstanceID {

	/**
	 * 该处理器绑定的副本地图 ID。
	 * Instance map id bound to the annotated handler.
	 *
	 * @return 地图 ID / map id
	 */
	int value();
}
