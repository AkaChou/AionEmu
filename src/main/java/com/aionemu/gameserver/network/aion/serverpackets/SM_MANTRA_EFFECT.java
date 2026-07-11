package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 真言/曼陀罗（Mantra）特效同步的服务端包。
 * Server packet that synchronizes mantra visual effects.
 *
 * @author Sweetkr
 */
public class SM_MANTRA_EFFECT extends AionServerPacket {

	private Player player;
	private int subEffectId;

	/**
	 * 构造真言特效包。
	 * Builds a mantra effect packet.
	 *
	 * casting player
	 * sub-effect id
	 */
	public SM_MANTRA_EFFECT(Player player, int subEffectId) {
		this.player = player;
		this.subEffectId = subEffectId;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(0x00);// 未知 / unk
		writeD(player.getObjectId());
		writeH(subEffectId);
	}
}
