package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Collection;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.skillengine.model.Effect;
import lombok.AllArgsConstructor;

/**
 * 向客户端同步自身异常状态与效果列表的服务端包。
 * Server packet synchronizing the local player's abnormal state and effect list to the client.
 */
@AllArgsConstructor
public class SM_ABNORMAL_STATE extends AionServerPacket {
	private final Collection<Effect> effects;
	private final int abnormals;

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
