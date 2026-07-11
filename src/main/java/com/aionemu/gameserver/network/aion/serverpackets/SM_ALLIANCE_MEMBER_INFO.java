package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.List;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.stats.container.PlayerLifeStats;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceMember;
import com.aionemu.gameserver.model.team2.common.legacy.PlayerAllianceEvent;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.world.WorldPosition;

/**
 * 向客户端同步联盟成员状态/事件（加入、离开、移动、重连等）的服务端包。
 * Server packet synchronizing alliance member status/events (join, leave, movement, reconnect, etc.) to the client.
 *
 * @author Sarynth (Thx Rhys2002 for Packets)
 */
public class SM_ALLIANCE_MEMBER_INFO extends AionServerPacket {

	private Player player;
	private PlayerAllianceEvent event;
	private final int allianceId;
	private final int objectId;

	/**
	 * 使用联盟成员与事件类型构造同步包。
	 * Creates a sync packet from an alliance member and event type.
	 *
	 * alliance member
	 * @param event 成员事件类型 / member event type
	 */
	public SM_ALLIANCE_MEMBER_INFO(PlayerAllianceMember member, PlayerAllianceEvent event) {
		this.player = member.getObject();
		this.event = event;
		this.allianceId = member.getAllianceId();
		this.objectId = member.getObjectId();
	}

	@Override
	protected void writeImpl(AionConnection con) {
		PlayerCommonData pcd = player.getCommonData();
		WorldPosition wp = player.getPosition();

		/**
	 * 确保成员断线且联盟槽位变更后仍显示为断线状态。 / Required so that when member is disconnected, and his playerAllianceGroup slot is changed, he will continue to appear as disconnected to the alliance
	 */
		if (event == PlayerAllianceEvent.ENTER && !player.isOnline()) {
			event = PlayerAllianceEvent.ENTER_OFFLINE;
		}

		writeD(allianceId);
		writeD(objectId);
		if (player.isOnline()) {
			PlayerLifeStats pls = player.getLifeStats();
			writeD(pls.getMaxHp());
			writeD(pls.getCurrentHp());
			writeD(pls.getMaxMp());
			writeD(pls.getCurrentMp());
			writeD(pls.getMaxFp());
			writeD(pls.getCurrentFp());
		} else {
			writeD(0);
			writeD(0);
			writeD(0);
			writeD(0);
			writeD(0);
			writeD(0);
		}

		writeD(0);// unk 3.5
		writeD(wp.getMapId());
		writeD(wp.getInstanceId());
		writeF(wp.getX());
		writeF(wp.getY());
		writeF(wp.getZ());
		writeC(pcd.getPlayerClass().getClassId());
		writeC(pcd.getGender().getGenderId());
		writeC(pcd.getLevel());
		writeC(this.event.getId());
		writeH(0); // channel 0x01?
		writeH(0);
		switch (this.event) {
		case LEAVE:
		case LEAVE_TIMEOUT:
		case BANNED:
		case MOVEMENT:
		case DISCONNECTED:
			break;

		case JOIN:
		case ENTER:
		case ENTER_OFFLINE:
		case UPDATE:
		case RECONNECT:
		case APPOINT_VICE_CAPTAIN: // Unused maybe...
		case DEMOTE_VICE_CAPTAIN:
		case APPOINT_CAPTAIN:
			writeS(pcd.getName());
			writeD(0); // retail reserves 64 bits here
			writeD(0);
			if (player.isOnline()) {
				List<Effect> abnormalEffects = player.getEffectController().getAbnormalEffects();
				writeC(127);
				writeH(abnormalEffects.size());
				if (abnormalEffects.size() > 0) {
					for (Effect effect : abnormalEffects) {
						writeD(effect.getEffectorId());
						writeH(effect.getSkillId());
						writeC(effect.getSkillLevel());
						writeC(effect.getTargetSlot());
						writeD(effect.getRemainingTime());
						writeH(0);
					}
				}
				writeB(new byte[32]);
			} else {
				List<Effect> abnormalEffects = player.getEffectController().getAbnormalEffects();
				writeC(0);
				writeH(abnormalEffects.size());
				if (abnormalEffects.size() > 0) {
					for (Effect effect : abnormalEffects) {
						writeD(effect.getEffectorId());
						writeH(effect.getSkillId());
						writeC(effect.getSkillLevel());
						writeC(effect.getTargetSlot());
						writeD(effect.getRemainingTime());
						writeH(0);
					}
				}
				writeB(new byte[32]);
			}
			break;
		case MEMBER_GROUP_CHANGE:
			writeS(pcd.getName());
			break;
		default:
			break;
		}
	}
}
