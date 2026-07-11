package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Collection;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 向客户端同步目标生物异常状态/效果列表的服务端包。
 * Server packet synchronizing a creature's abnormal effect list to the client.
 */
public class SM_ABNORMAL_EFFECT extends AionServerPacket {
	private int effectedId;
	private int effectType = 1;
	private int abnormals;
	private Collection<Effect> filtered;

	/**
	 * 构造异常效果同步包；若目标为玩家则使用玩家效果格式。
	 * Builds an abnormal-effect sync packet; uses the player effect format when the target is a player.
	 *
	 * @param effected 受效果影响的生物 / creature under the effects
	 * @param abnormals 异常状态位掩码 / abnormal state bit mask
	 * @param effects 需要同步的效果集合 / effects to synchronize
	 */
	public SM_ABNORMAL_EFFECT(Creature effected, int abnormals, Collection<Effect> effects) {
		this.abnormals = abnormals;
		this.effectedId = effected.getObjectId();
		this.filtered = effects;
		if (effected instanceof Player) {
			effectType = 2;
		}
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(effectedId);
		writeC(effectType); // 未知 / unk
		writeD(0); // 未知 / unk
		writeD(abnormals); // 未知 / unk
		writeD(0); // 未知 / unk
		writeC(0x7F);// unk 5.3
		writeH(filtered.size()); // effects size
		for (Effect effect : filtered) {
			switch (effectType) {
			case 2:
				writeD(effect.getEffectorId());
				writeH(effect.getSkillId());
				writeC(effect.getSkillLevel());
				writeC(effect.getTargetSlot());
				writeD(effect.getRemainingTime());
				writeH(0x00);// unk 5.3
				break;
			case 1:
				writeH(effect.getSkillId());
				writeC(effect.getSkillLevel());
				writeC(effect.getTargetSlot());
				writeD(effect.getRemainingTime());
				writeH(0x00);// unk 5.3
				break;
			default:
				writeH(effect.getSkillId());
				writeC(effect.getSkillLevel());
			}
		}
	}
}
