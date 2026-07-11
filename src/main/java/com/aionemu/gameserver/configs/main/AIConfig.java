package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * AI 行为相关配置。
 * AI behavior related configuration.
 *
 * @author ATracer
 */
public class AIConfig {

	/**
	 * 是否启用 AI 移动调试日志。
	 * Whether AI move debug logging is enabled.
	 */
	@Property(key = "gameserver.ai.move.debug", defaultValue = "true")
	public static boolean MOVE_DEBUG;

	/**
	 * 是否启用 AI 事件调试日志。
	 * Whether AI event debug logging is enabled.
	 */
	@Property(key = "gameserver.ai.event.debug", defaultValue = "false")
	public static boolean EVENT_DEBUG;

	/**
	 * 是否启用 AI 创建时调试日志。
	 * Whether AI on-create debug logging is enabled.
	 */
	@Property(key = "gameserver.ai.oncreate.debug", defaultValue = "false")
	public static boolean ONCREATE_DEBUG;

	/**
	 * 对仇恨免疫的等级差阈值。
	 * Level difference to be immune to aggro.
	 */
	@Property(key = "gameserver.ai.aggro.level.immune", defaultValue = "10")
	public static int AGGRO_LEVEL_IMMUNE;

	/**
	 * 是否启用 NPC 移动。
	 * Whether NPC movement is enabled.
	 */
	@Property(key = "gameserver.npcmovement.enable", defaultValue = "true")
	public static boolean ACTIVE_NPC_MOVEMENT;

	/**
	 * NPC 移动最小延迟（秒）。
	 * Minimum NPC movement delay in seconds.
	 */
	@Property(key = "gameserver.npcmovement.delay.minimum", defaultValue = "3")
	public static int MINIMIMUM_DELAY;

	/**
	 * NPC 移动最大延迟（秒）。
	 * Maximum NPC movement delay in seconds.
	 */
	@Property(key = "gameserver.npcmovement.delay.maximum", defaultValue = "15")
	public static int MAXIMUM_DELAY;

	/**
	 * 是否启用 NPC 喊话。
	 * Whether NPC shouts are enabled.
	 */
	@Property(key = "gameserver.npcshouts.enable", defaultValue = "false")
	public static boolean SHOUTS_ENABLE;
}
