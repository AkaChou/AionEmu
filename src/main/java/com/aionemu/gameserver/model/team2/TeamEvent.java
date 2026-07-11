package com.aionemu.gameserver.model.team2;

/**
 * 团队活动接口。
 * Team Event interface.
 *
 * @author ATracer
 */
public interface TeamEvent {

	void handleEvent();

	boolean checkCondition();
}
