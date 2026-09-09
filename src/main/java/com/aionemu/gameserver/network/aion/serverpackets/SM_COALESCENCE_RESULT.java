package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 融合（Coalescence）结果包：返回产出物品与可选奖励信息。
 * Server packet for coalescence result: resulting item and optional bonus reward.
 *
 * @author Ranastic
 */
@AllArgsConstructor
public class SM_COALESCENCE_RESULT extends AionServerPacket {
	private final int itemTemplateId;
	private final int itemObjId;
	private final int bonusTemplateId;
	private final long bonusCount;
	private final boolean isBonus;

	@Override
	protected void writeImpl(AionConnection client) {
		writeD(itemTemplateId);
		writeD(itemObjId);
		writeD(bonusTemplateId);
		writeD(isBonus ? 1 : 0);
		writeQ(bonusCount);
	}
}
