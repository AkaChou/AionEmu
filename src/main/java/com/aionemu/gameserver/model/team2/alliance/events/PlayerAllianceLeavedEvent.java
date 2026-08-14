package com.aionemu.gameserver.model.team2.alliance.events;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceMember;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceService;
import com.aionemu.gameserver.model.team2.common.events.PlayerLeavedEvent;
import com.aionemu.gameserver.model.team2.common.legacy.PlayerAllianceEvent;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_MEMBER_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEAVE_GROUP_MEMBER;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

/**
 * 玩家联盟 Leaved 活动，用于团队2相关逻辑。
 * Player Alliance Leaved Event for team 2 logic.
 *
 * @author ATracer
 */
public class PlayerAllianceLeavedEvent extends PlayerLeavedEvent<PlayerAllianceMember, PlayerAlliance> {

	public PlayerAllianceLeavedEvent(PlayerAlliance alliance, Player player) {
		super(alliance, player);
	}

	public PlayerAllianceLeavedEvent(PlayerAlliance team, Player player, PlayerLeavedEvent.LeaveReson reason,
			String banPersonName) {
		super(team, player, reason, banPersonName);
	}

	public PlayerAllianceLeavedEvent(PlayerAlliance alliance, Player player, PlayerLeavedEvent.LeaveReson reason) {
		super(alliance, player, reason);
	}

	/** 处理活动。 / Handle event. */
	@Override
	public void handleEvent() {
		team.removeMember(leavedPlayer.getObjectId());
		team.getViceCaptainIds().remove(leavedPlayer.getObjectId());

		if (leavedPlayer.isOnline()) {
			PacketSendUtility.sendPacket(leavedPlayer, new SM_LEAVE_GROUP_MEMBER());
		}

		team.apply(this);

		switch (reason) {
		case BAN:
		case LEAVE:
		case LEAVE_TIMEOUT:
			if (team.getTeamType().shouldDisband(team.onlineMembers())) {
				PlayerAllianceService.disband(team);
			} else if (team.onlineMembers() > 0 && leavedPlayer.equals(team.getLeader().getObject())) {
				team.onEvent(new ChangeAllianceLeaderEvent(team));
			}
			if (reason == LeaveReson.BAN) {
				PacketSendUtility.sendPacket(leavedPlayer, SM_SYSTEM_MESSAGE.STR_FORCE_BAN_ME(banPersonName));
			}

			break;
		case DISBAND:
			PacketSendUtility.sendPacket(leavedPlayer, SM_SYSTEM_MESSAGE.STR_PARTY_ALLIANCE_DISPERSED);
			break;
		}

		if (leavedPlayer.isInInstance()) {
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/** 运行 / Run. */
				@Override
				public void run() {
					if (!leavedPlayer.isInAlliance2()) {
						WorldMapInstance instance = leavedPlayer.getPosition().getWorldMapInstance();
						if (instance.getRegistredAlliance() != null || instance.getRegistredLeague() != null) {
							InstanceService.moveToExitPoint(leavedPlayer);
						}
					}
				}
			}, 10000);
		}
	}

	/** 应用。 / Apply. */
	@Override
	public boolean apply(PlayerAllianceMember member) {
		Player player = member.getObject();

		PacketSendUtility.sendPacket(player, new SM_ALLIANCE_MEMBER_INFO(leavedTeamMember, PlayerAllianceEvent.LEAVE));
		PacketSendUtility.sendPacket(player, new SM_ALLIANCE_INFO(team));

		switch (reason) {
		case LEAVE_TIMEOUT:
			PacketSendUtility.sendPacket(player,
					SM_SYSTEM_MESSAGE.STR_PARTY_ALLIANCE_HE_LEAVED_PARTY(leavedPlayer.getName()));
			break;
		case LEAVE:
			PacketSendUtility.sendPacket(player,
					SM_SYSTEM_MESSAGE.STR_PARTY_ALLIANCE_HE_LEAVED_PARTY(leavedPlayer.getName()));
			break;
		case DISBAND:
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_PARTY_ALLIANCE_DISPERSED);
			break;
		case BAN:
			PacketSendUtility.sendPacket(player,
					SM_SYSTEM_MESSAGE.STR_FORCE_BAN_HIM(banPersonName, leavedPlayer.getName()));
			break;
		}
		return true;
	}
}
