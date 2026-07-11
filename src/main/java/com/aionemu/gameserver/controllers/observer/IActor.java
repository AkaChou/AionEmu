package com.aionemu.gameserver.controllers.observer;

/**
 * 碰撞/区域行为执行者接口。
 * Actor interface for collision or zone-triggered behavior.
 *
 * @author Rolandas
 */
public interface IActor {

	/**
	 * 触发行为（如死亡、施加材质技能）。
	 * Trigger the actor behavior (e.g. die, apply material skills).
	 */
	void act();

	/**
	 * 启用或禁用该行为者。
	 * Enable or disable this actor.
	 *
	 * whether to enable
	 */
	void setEnabled(boolean enable);

	/**
	 * 中止当前行为（如取消定时任务）。
	 * Abort the current behavior (e.g. cancel scheduled tasks).
	 */
	void abort();
}
