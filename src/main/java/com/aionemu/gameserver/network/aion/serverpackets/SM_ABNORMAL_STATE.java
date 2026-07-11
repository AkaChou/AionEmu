package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Collection;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 向客户端同步自身异常状态与效果列表的服务端包。
 * Server packet synchronizing the local player's abnormal state and effect list to the client.
 */
public class SM_ABNORMAL_STATE extends AionServerPacket {
	private Collection<Effect> effects;
	private int abnormals;

	/**
	 * 使用异常位掩码与效果集合构造同步包。
	 * Creates a sync packet from an abnormal bit mask and effect collection.
	 *
	 * @param effects 需要同步的效果集合 / effects to synchronize
	 * @param abnormals 异常状态位掩码 / abnormal state bit mask
	 */
	public SM_ABNORMAL_STATE(Collection<Effect> effects, int abnormals) {
		this.effects = effects;
		this.abnormals = abnormals;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(abnormals);
		writeD(0);
		writeD(0);// unk 4.5
		writeC(0x7F);// unk 4.5(127)what's that? O.o
		writeH(effects.size());
		for (Effect effect : effects) {
			writeD(effect.getEffectorId());
			writeH(effect.getSkillId());
			writeC(effect.getSkillLevel());
			writeC(effect.getTargetSlot());
			writeD(effect.getRemainingTime());
			writeH(0x00); // unk 5.3
		}
	}
}
