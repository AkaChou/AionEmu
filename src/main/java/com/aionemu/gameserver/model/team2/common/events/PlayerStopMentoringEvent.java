package com.aionemu.gameserver.model.team2.common.events;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.TeamMember;
import com.aionemu.gameserver.model.team2.TemporaryPlayerTeam;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANK_UPDATE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.google.common.base.Predicate;

/**
 * 玩家 StopMentoring 活动，用于团队2相关逻辑。
 * Player Stop Mentoring Event for team 2 logic.
 *
 * @author ATracer
 */
public abstract class PlayerStopMentoringEvent<T extends TemporaryPlayerTeam<? extends TeamMember<Player>>>
		extends AlwaysTrueTeamEvent implements Predicate<Player> {

	protected final T team;
	protected final Player player;

	public PlayerStopMentoringEvent(T team, Player player) {
		this.team = team;
		this.player = player;
	}

	/** 处理活动。 / Handle event. */
	@Override
	public void handleEvent() {
		player.setMentor(false);
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_MENTOR_END);
		team.applyOnMembers(this);
		PacketSendUtility.broadcastPacketAndReceive(player, new SM_ABYSS_RANK_UPDATE(2, player));
	}

	/** 应用。 / Apply. */
	@Override
	public boolean apply(Player member) {
		if (!player.equals(member)) {
			PacketSendUtility.sendPacket(member, SM_SYSTEM_MESSAGE.STR_MSG_MENTOR_END_PARTYMSG(player.getName()));
		}
		sendGroupPacketOnMentorEnd(member);
		return true;
	}

	/**
	 * @param member 队伍成员 / team member
	 */
	protected abstract void sendGroupPacketOnMentorEnd(Player member);
}
