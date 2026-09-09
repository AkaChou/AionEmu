package com.aionemu.gameserver.model.team2.group.events;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.common.events.AlwaysTrueTeamEvent;
import com.aionemu.gameserver.model.team2.common.events.PlayerLeavedEvent.LeaveReson;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.google.common.base.Predicate;
import lombok.AllArgsConstructor;

/**
 * 队伍解散事件（团队2）。
 * Group Disband Event for team 2 logic.
 *
 * @author ATracer
 */
@AllArgsConstructor
public class GroupDisbandEvent extends AlwaysTrueTeamEvent implements Predicate<Player> {

	private final PlayerGroup group;

	/** 处理事件。 / Handle event. */
	@Override
	public void handleEvent() {
		group.applyOnMembers(this);
	}

	/** 应用。 / Apply. */
	@Override
	public boolean apply(Player player) {
		group.onEvent(new PlayerGroupLeavedEvent(group, player, LeaveReson.DISBAND));
		return true;
	}
}
