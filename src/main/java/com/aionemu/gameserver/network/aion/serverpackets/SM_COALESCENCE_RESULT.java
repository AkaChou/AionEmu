package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 融合（Coalescence）结果包：返回产出物品与可选奖励信息。
 * Server packet for coalescence result: resulting item and optional bonus reward.
 *
 * @author Ranastic
 */
public class SM_COALESCENCE_RESULT extends AionServerPacket {
	private int itemTemplateId;
	private int itemObjId;
	private int bonusTemplateId;
	private long bonusCount;
	private boolean isBonus;

	/**
	 * 构造融合结果包。
	 * Creates a coalescence result packet.
	 *
	 * @param itemTemplateId 产出物品模板 ID / resulting item template id
	 * @param itemObjId 产出物品对象 ID / resulting item object id
	 * @param bonusTemplateId 额外奖励模板 ID，无奖励时为 0 / bonus template id, 0 if none
	 * @param bonusCount 额外奖励数量 / bonus count
	 * @param isBonus 是否附带额外奖励 / whether a bonus reward is included
	 */
	public SM_COALESCENCE_RESULT(int itemTemplateId, int itemObjId, int bonusTemplateId, long bonusCount,
			boolean isBonus) {
		this.itemTemplateId = itemTemplateId;
		this.itemObjId = itemObjId;
		this.bonusTemplateId = bonusTemplateId;
		this.bonusCount = bonusCount;
		this.isBonus = isBonus;
	}

	@Override
	protected void writeImpl(AionConnection client) {
		writeD(itemTemplateId);
		writeD(itemObjId);
		writeD(bonusTemplateId);
		writeD(isBonus ? 1 : 0);
		writeQ(bonusCount);
	}
}
