package com.aionemu.gameserver.model.team2.group.events;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.common.events.AlwaysTrueTeamEvent;
import com.aionemu.gameserver.model.team2.common.events.PlayerLeavedEvent.LeaveReson;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.google.common.base.Predicate;

/**
 * 队伍 Disband 活动，用于团队2相关逻辑。
 * Group Disband Event for team 2 logic.
 *
 * @author ATracer
 */
public class GroupDisbandEvent extends AlwaysTrueTeamEvent implements Predicate<Player> {

	private final PlayerGroup group;

	/**
	 * @param group
	 */
	public GroupDisbandEvent(PlayerGroup group) {
		this.group = group;
	}

	/** 处理活动。 / Handle event. */
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
