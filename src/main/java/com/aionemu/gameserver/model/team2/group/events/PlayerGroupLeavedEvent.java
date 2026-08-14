package com.aionemu.gameserver.model.team2.group.events;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.common.events.PlayerLeavedEvent;
import com.aionemu.gameserver.model.team2.common.legacy.GroupEvent;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.model.team2.group.PlayerGroupMember;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.network.aion.serverpackets.SM_GROUP_MEMBER_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEAVE_GROUP_MEMBER;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 玩家离队事件（团队2）。
 * Player Group Leaved Event for team 2 logic.
 *
 * @author ATracer
 */
public class PlayerGroupLeavedEvent extends PlayerLeavedEvent<PlayerGroupMember, PlayerGroup> {

	public PlayerGroupLeavedEvent(PlayerGroup alliance, Player player) {
		super(alliance, player);
	}

	public PlayerGroupLeavedEvent(PlayerGroup team, Player player, PlayerLeavedEvent.LeaveReson reason,
			String banPersonName) {
		super(team, player, reason, banPersonName);
	}

	public PlayerGroupLeavedEvent(PlayerGroup alliance, Player player, PlayerLeavedEvent.LeaveReson reason) {
		super(alliance, player, reason);
	}

	/** 处理事件。 / Handle event. */
	@Override
	public void handleEvent() {
		team.removeMember(leavedPlayer.getObjectId());

		if (leavedPlayer.isMentor()) {
			team.onEvent(new PlayerGroupStopMentoringEvent(team, leavedPlayer));
		}
		team.apply(this);

		PacketSendUtility.sendPacket(leavedPlayer, new SM_LEAVE_GROUP_MEMBER());
		switch (reason) {
		case BAN:
		case LEAVE:
			// PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_PARTY_SECEDE); //
			// 客户端侧？ / client side?
			if (team.getTeamType().shouldDisband(team.onlineMembers())) {
				PlayerGroupService.disband(team);
			} else if (team.onlineMembers() > 0 && leavedPlayer.equals(team.getLeader().getObject())) {
				team.onEvent(new ChangeGroupLeaderEvent(team));
			}
			if (reason == LeaveReson.BAN) {
				PacketSendUtility.sendPacket(leavedPlayer, SM_SYSTEM_MESSAGE.STR_PARTY_YOU_ARE_BANISHED);
			}
			break;
		case DISBAND:
			PacketSendUtility.sendPacket(leavedPlayer, SM_SYSTEM_MESSAGE.STR_PARTY_IS_DISPERSED);
			break;
		}

		if (leavedPlayer.isInInstance()) {
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/** 运行 / run. */
				@Override
				public void run() {
					if (!leavedPlayer.isInGroup2()) {
						if (leavedPlayer.getPosition().getWorldMapInstance().getRegisteredGroup() != null) {
							InstanceService.moveToExitPoint(leavedPlayer);
						}
					}
				}
			}, 10000);
		}
	}

	/** 应用。 / Apply. */
	@Override
	public boolean apply(PlayerGroupMember member) {
		Player player = member.getObject();
		PacketSendUtility.sendPacket(player, new SM_GROUP_MEMBER_INFO(team, leavedPlayer, GroupEvent.LEAVE));

		switch (reason) {
		case LEAVE:
		case DISBAND:
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_PARTY_HE_LEAVE_PARTY(leavedPlayer.getName()));
			break;
		case BAN:
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_PARTY_HE_IS_BANISHED(leavedPlayer.getName()));
			break;
		}
		return true;
	}
}
