package com.aionemu.gameserver.model.team2.league.events;

import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;
import com.aionemu.gameserver.model.team2.common.events.AlwaysTrueTeamEvent;
import com.aionemu.gameserver.model.team2.league.League;
import com.aionemu.gameserver.model.team2.league.events.LeagueLeftEvent.LeaveReson;
import com.google.common.base.Predicate;

/**
 * 战团解散事件，用于团队2相关逻辑。
 * League Disband Event for team 2 logic.
 */

public class LeagueDisbandEvent extends AlwaysTrueTeamEvent implements Predicate<PlayerAlliance> {
	private final League league;

	public LeagueDisbandEvent(League league) {
		this.league = league;
	}

	/** 处理活动。 / Handle event. */
	@Override
	public void handleEvent() {
		league.applyOnMembers(this);
	}

	/** 应用。 / Apply. */
	@Override
	public boolean apply(PlayerAlliance alliance) {
		league.onEvent(new LeagueLeftEvent(league, alliance, LeaveReson.DISBAND));
		return true;
	}
}
