package com.aionemu.gameserver.model.team2.group.events;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.common.events.PlayerStopMentoringEvent;
import com.aionemu.gameserver.model.team2.common.legacy.GroupEvent;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.network.aion.serverpackets.SM_GROUP_MEMBER_INFO;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 玩家停止指导事件（团队2）。
 * Player Group Stop Mentoring Event for team 2 logic.
 *
 * @author ATracer
 */
public class PlayerGroupStopMentoringEvent extends PlayerStopMentoringEvent<PlayerGroup> {

	/**
	 * 以队伍与玩家构造停止指导事件。
	 * Constructs a stop-mentoring event with group and player.
	 *
	 * @param group 目标队伍 / target group
	 * @param player 相关玩家 / related player
	 */
	public PlayerGroupStopMentoringEvent(PlayerGroup group, Player player) {
		super(group, player);
	}

	@Override
	protected void sendGroupPacketOnMentorEnd(Player member) {
		PacketSendUtility.sendPacket(member, new SM_GROUP_MEMBER_INFO(team, player, GroupEvent.MOVEMENT));
	}
}
