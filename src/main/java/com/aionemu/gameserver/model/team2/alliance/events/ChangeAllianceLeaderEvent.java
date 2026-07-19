package com.aionemu.gameserver.model.team2.alliance.events;

import java.util.Collection;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceMember;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceService;
import com.aionemu.gameserver.model.team2.alliance.events.AssignViceCaptainEvent.AssignType;
import com.aionemu.gameserver.model.team2.common.events.ChangeLeaderEvent;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.google.common.base.Predicate;

/**
 * Change 联盟 Leader 活动，用于团队2相关逻辑。
 * Change Alliance Leader Event for team 2 logic.
 */

public class ChangeAllianceLeaderEvent extends ChangeLeaderEvent<PlayerAlliance> {
	public ChangeAllianceLeaderEvent(PlayerAlliance team, Player eventPlayer) {
		super(team, eventPlayer);
	}

	public ChangeAllianceLeaderEvent(PlayerAlliance team) {
		super(team, null);
	}

	/** 处理活动。 / Handle event. */
	@Override
	public void handleEvent() {
		Player oldLeader = team.getLeaderObject();
		if (eventPlayer == null) {
			Collection<Integer> viceCaptainIds = team.getViceCaptainIds();
			for (Integer viceCaptainId : viceCaptainIds) {
				PlayerAllianceMember viceCaptain = team.getMember(viceCaptainId);
				if (viceCaptain.isOnline()) {
					changeLeaderTo(viceCaptain.getObject());
					viceCaptainIds.remove(viceCaptainId);
					break;
				}
			}
			if (team.isLeader(oldLeader)) {
				team.applyOnMembers(this);
			}
		} else {
			changeLeaderTo(eventPlayer);
		}
		checkLeaderChanged(oldLeader);
		if (eventPlayer != null) {
			PlayerAllianceService.changeViceCaptain(oldLeader, AssignType.DEMOTE_CAPTAIN_TO_VICECAPTAIN);
		}
		GameRuntimeServices.findGroupService().onTeamChanged(team);
	}

	@Override
	protected void changeLeaderTo(final Player player) {
		team.changeLeader(team.getMember(player.getObjectId()));
		team.applyOnMembers(new Predicate<Player>() {
			/** 应用。 / Apply. */
			@Override
			public boolean apply(Player member) {
				PacketSendUtility.sendPacket(member, new SM_ALLIANCE_INFO(team));
				if (!player.equals(member)) {
					PacketSendUtility.sendPacket(member,
							SM_SYSTEM_MESSAGE.STR_FORCE_HE_IS_NEW_LEADER(player.getName()));
				} else {
					PacketSendUtility.sendPacket(member, SM_SYSTEM_MESSAGE.STR_FORCE_YOU_BECOME_NEW_LEADER);
				}
				return true;
			}
		});
	}
}
