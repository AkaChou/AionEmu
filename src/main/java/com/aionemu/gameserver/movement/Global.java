package com.aionemu.gameserver.movement;

import com.aionemu.gameserver.movement.processors.movement.MovementProcessor;

/**
 * 移动子系统全局入口，持有共享的 {@link MovementProcessor} 单例。
 * Global entry for the movement subsystem, holding the shared {@link MovementProcessor} singleton.
 *
 * @author MetaWind
 */
public class Global {

	/**
	 * 全局移动处理器实例。
	 * Global movement-processor instance.
	 */
	public static final MovementProcessor MovementProcessor = new MovementProcessor();
}
