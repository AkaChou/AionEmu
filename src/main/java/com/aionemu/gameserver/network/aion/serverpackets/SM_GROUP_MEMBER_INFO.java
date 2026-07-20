package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.List;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.stats.container.PlayerLifeStats;
import com.aionemu.gameserver.model.team2.common.legacy.GroupEvent;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.world.WorldPosition;

/**
 * 向客户端同步小队成员状态（生命值、位置、职业与异常状态等）的服务端包。
 * Server packet that synchronizes group member state (vitals, position, class, and abnormals) to the client.
 */
public class SM_GROUP_MEMBER_INFO extends AionServerPacket {
	private int groupId;
	private Player player;
	private GroupEvent event;

	/**
	 * @param group 玩家小队 / Player group
	 * Target member
	 * @param event 触发的小队事件类型 / Group event that triggered the update
	 */
	public SM_GROUP_MEMBER_INFO(PlayerGroup group, Player player, GroupEvent event) {
		this.groupId = group.getTeamId();
		this.player = player;
		this.event = event;
	}

	/**
	 * 按事件类型写入成员生命值、坐标、职业、在线状态及异常效果列表。
	 * Writes member vitals, coordinates, class, online state, and abnormal effect lists by event type.
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		PlayerLifeStats pls = player.getLifeStats();
		PlayerCommonData pcd = player.getCommonData();
		WorldPosition wp = pcd.getPosition();
		if (event == GroupEvent.ENTER && !player.isOnline()) {
			event = GroupEvent.ENTER_OFFLINE;
		}
		writeD(groupId);
		writeD(player.getObjectId());
		if (player.isOnline()) {
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
		writeD(0);
		writeD(wp.getMapId());
		writeD(wp.getInstanceId());
		writeF(wp.getX());
		writeF(wp.getY());
		writeF(wp.getZ());
		writeC(pcd.getPlayerClass().getClassId());
		writeC(pcd.getGender().getGenderId());
		writeC(pcd.getLevel());
		writeC(event.getId());
		writeH(player.isOnline() ? 1 : 0);
		writeC(player.isMentor() ? 0x01 : 0x00);
		writeC(0x00);// unk 5.3
		switch (event) {
		case MOVEMENT:
		case DISCONNECTED:
			break;
		case LEAVE:
			break;
		case ENTER_OFFLINE:
		case JOIN:
			writeS(pcd.getName());
			break;
		case UPDATE:
			writeS(pcd.getName());
			writeD(0x00);
			writeD(0x00);
			writeC(0x7F); // all effect slots
			List<Effect> abnormalEffects1 = this.player.getEffectController().getAbnormalEffects();
			writeH(abnormalEffects1.size());
			for (Effect effect : abnormalEffects1) {
				writeD(effect.getEffectorId());
				writeH(effect.getSkillId());
				writeC(effect.getSkillLevel());
				writeC(effect.getTargetSlot());
				writeD(effect.getRemainingTime());
				writeH(0x00);// unk 5.3
			}
			writeB("3743000037430000374300003743000037430000374300003743000037430000");// 32bytes
			break;
		case UNK_53:
			writeD(0x00);
			writeD(0x00);
			writeC(0x7F); // all effect slots
			List<Effect> abnormalEffects2 = this.player.getEffectController().getAbnormalEffects();
			writeH(abnormalEffects2.size());
			for (Effect effect : abnormalEffects2) {
				writeD(effect.getEffectorId());
				writeH(effect.getSkillId());
				writeC(effect.getSkillLevel());
				writeC(effect.getTargetSlot());
				writeD(effect.getRemainingTime());
				writeH(0x00);// unk 5.3
			}
			writeB("1901000011B9000011B9000011B9000011B9000011B9000011B9000000000000");// 32bytes
			break;
		default:
			writeS(pcd.getName());
			writeD(0x00);
			writeD(0x00);
			writeC(0x7F); // all effect slots
			List<Effect> abnormalEffects = player.getEffectController().getAbnormalEffects();
			writeH(abnormalEffects.size());
			for (Effect effect : abnormalEffects) {
				writeD(effect.getEffectorId());
				writeH(effect.getSkillId());
				writeC(effect.getSkillLevel());
				writeC(effect.getTargetSlot());
				writeD(effect.getRemainingTime());
				writeH(0x00);// unk 5.3
			}
			writeB(new byte[32]);
			break;
		}
	}
}
