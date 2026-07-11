package com.aionemu.gameserver.model.team2.common.events;

import org.apache.commons.lang3.StringUtils;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.TeamEvent;
import com.aionemu.gameserver.model.team2.TeamMember;
import com.aionemu.gameserver.model.team2.TemporaryPlayerTeam;
import com.google.common.base.Predicate;

/**
 * 玩家 Leaved 活动，用于团队2相关逻辑。
 * Player Leaved Event for team 2 logic.
 */

public abstract class PlayerLeavedEvent<TM extends TeamMember<Player>, T extends TemporaryPlayerTeam<TM>>
		implements Predicate<TM>, TeamEvent {
	public static enum LeaveReson {
		BAN, LEAVE, LEAVE_TIMEOUT, DISBAND;
	}

	protected final T team;
	protected final Player leavedPlayer;
	protected final LeaveReson reason;
	protected final TM leavedTeamMember;
	protected final String banPersonName;

	public PlayerLeavedEvent(T alliance, Player player) {
		this(alliance, player, LeaveReson.LEAVE);
	}

	public PlayerLeavedEvent(T alliance, Player player, LeaveReson reason) {
		this(alliance, player, reason, StringUtils.EMPTY);
	}

	public PlayerLeavedEvent(T team, Player player, LeaveReson reason, String banPersonName) {
		this.team = team;
		this.leavedPlayer = player;
		this.reason = reason;
		this.leavedTeamMember = team.getMember(player.getObjectId());
		this.banPersonName = banPersonName;
	}

	/**
	 * @return Check condition / Check condition
	 */
	@Override
	public boolean checkCondition() {
		return team.hasMember(leavedPlayer.getObjectId());
	}
}
