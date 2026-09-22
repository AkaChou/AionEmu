package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 真言/曼陀罗（Mantra）特效同步的服务端包。
 * Server packet that synchronizes mantra visual effects.
 * @author Sweetkr
 */
@AllArgsConstructor
public class SM_MANTRA_EFFECT extends AionServerPacket {

	private final Player player;
	private final int subEffectId;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(0x00);// 未知 / unk
		writeD(player.getObjectId());
		writeH(subEffectId);
	}
}
