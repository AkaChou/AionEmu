package com.aionemu.gameserver.model.team2.common.events;

import com.aionemu.gameserver.model.team2.TeamEvent;

/**
 * AlwaysTrue 团队活动，用于团队2相关逻辑。
 * Always True Team Event for team 2 logic.
 *
 * @author ATracer
 */
public abstract class AlwaysTrueTeamEvent implements TeamEvent {

	/**
	 * @return Check condition
	 */
	@Override
	public final boolean checkCondition() {
		return true;
	}
}
