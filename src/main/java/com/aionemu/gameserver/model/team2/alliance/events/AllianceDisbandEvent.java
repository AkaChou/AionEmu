package com.aionemu.gameserver.model.team2.alliance.events;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;
import com.aionemu.gameserver.model.team2.common.events.AlwaysTrueTeamEvent;
import com.aionemu.gameserver.model.team2.common.events.PlayerLeavedEvent.LeaveReson;
import com.google.common.base.Predicate;

/**
 * 联盟解散事件。
 * Alliance Disband Event.
 */

public class AllianceDisbandEvent extends AlwaysTrueTeamEvent implements Predicate<Player> {
	private final PlayerAlliance alliance;

	public AllianceDisbandEvent(PlayerAlliance alliance) {
		this.alliance = alliance;
	}

	/** 处理活动。 / Handle event. */
	@Override
	public void handleEvent() {
		alliance.applyOnMembers(this);
	}

	/** 应用。 / Apply. */
	@Override
	public boolean apply(Player player) {
		alliance.onEvent(new PlayerAllianceLeavedEvent(alliance, player, LeaveReson.DISBAND));
		return true;
	}
}
