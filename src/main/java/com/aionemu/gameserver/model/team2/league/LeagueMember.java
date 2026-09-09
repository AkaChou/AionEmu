package com.aionemu.gameserver.model.team2.league;

import com.aionemu.gameserver.model.team2.TeamMember;
import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;
import lombok.AllArgsConstructor;

/**
 * 战团成员，用于团队 2 相关逻辑。
 * League member for team 2 logic.
 *
 * @author ATracer
 */
@AllArgsConstructor
public class LeagueMember implements TeamMember<PlayerAlliance> {
	private final PlayerAlliance alliance;
	private final int leaguePosition;

	/** 返回对象 ID / Returns the object id */
	@Override
	public Integer getObjectId() {
		return alliance.getObjectId();
	}

	/** 获取名称。 / Returns the name. */
	@Override
	public String getName() {
		return alliance.getName();
	}

	/** 获取对象。 / Returns the object. */
	@Override
	public PlayerAlliance getObject() {
		return alliance;
	}

	/** 获取战团坐标。 / Returns the league position. */
	public final int getLeaguePosition() {
		return leaguePosition;
	}
}
