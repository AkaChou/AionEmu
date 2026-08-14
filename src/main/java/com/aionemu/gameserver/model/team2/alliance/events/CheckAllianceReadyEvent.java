package com.aionemu.gameserver.model.team2.alliance.events;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;
import com.aionemu.gameserver.model.team2.common.events.AlwaysTrueTeamEvent;
import com.aionemu.gameserver.model.team2.common.events.TeamCommand;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_READY_CHECK;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.google.common.base.Predicate;

/**
 * 联盟就绪检查事件。
 * Check Alliance Ready Event.
 *
 * @author ATracer
 */
public class CheckAllianceReadyEvent extends AlwaysTrueTeamEvent implements Predicate<Player> {

	private final PlayerAlliance alliance;
	private final Player player;
	private final TeamCommand eventCode;

	public CheckAllianceReadyEvent(PlayerAlliance alliance, Player player, TeamCommand eventCode) {
		this.alliance = alliance;
		this.player = player;
		this.eventCode = eventCode;
	}

	/** 处理活动。 / Handle event. */
	@Override
	public void handleEvent() {
		int readyStatus = alliance.getAllianceReadyStatus();
		switch (eventCode) {
		case ALLIANCE_CHECKREADY_CANCEL:
			readyStatus = 0;
			break;
		case ALLIANCE_CHECKREADY_START:
			readyStatus = alliance.onlineMembers() - 1;
			break;
		case ALLIANCE_CHECKREADY_AUTOCANCEL:
			readyStatus = 0;
			break;
		case ALLIANCE_CHECKREADY_READY:
		case ALLIANCE_CHECKREADY_NOTREADY:
			readyStatus -= 1;
			break;
		default:
			break;
		}
		alliance.setAllianceReadyStatus(readyStatus);
		alliance.applyOnMembers(this);
	}

	/** 应用。 / Apply. */
	@Override
	public boolean apply(Player member) {
		switch (eventCode) {
		case ALLIANCE_CHECKREADY_CANCEL:
			PacketSendUtility.sendPacket(member, new SM_ALLIANCE_READY_CHECK(player.getObjectId(), 0));
			break;
		case ALLIANCE_CHECKREADY_START:
			PacketSendUtility.sendPacket(member, new SM_ALLIANCE_READY_CHECK(player.getObjectId(), 5));
			PacketSendUtility.sendPacket(member, new SM_ALLIANCE_READY_CHECK(player.getObjectId(), 1));
			break;
		case ALLIANCE_CHECKREADY_AUTOCANCEL:
			PacketSendUtility.sendPacket(member, new SM_ALLIANCE_READY_CHECK(player.getObjectId(), 2));
			break;
		case ALLIANCE_CHECKREADY_READY:
			PacketSendUtility.sendPacket(member, new SM_ALLIANCE_READY_CHECK(player.getObjectId(), 5));
			if (alliance.getAllianceReadyStatus() == 0) {
				PacketSendUtility.sendPacket(member, new SM_ALLIANCE_READY_CHECK(0, 3));
			}
			break;
		case ALLIANCE_CHECKREADY_NOTREADY:
			PacketSendUtility.sendPacket(member, new SM_ALLIANCE_READY_CHECK(player.getObjectId(), 4));
			if (alliance.getAllianceReadyStatus() == 0) {
				PacketSendUtility.sendPacket(member, new SM_ALLIANCE_READY_CHECK(0, 3));
			}
			break;
		default:
			break;
		}
		return true;
	}
}
