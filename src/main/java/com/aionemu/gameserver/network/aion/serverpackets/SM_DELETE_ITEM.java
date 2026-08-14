package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemDeleteType;

/**
 * 删除背包物品包：按对象 ID 与删除类型移除物品。
 * Server packet that deletes an inventory item by object id and delete type.
 *
 * @author Avol
 */
public class SM_DELETE_ITEM extends AionServerPacket {

	private final int itemObjectId;
	private final ItemDeleteType deleteType;

	/**
	 * 以任务奖励删除类型构造物品删除包。
	 * Creates an item deletion packet with the quest-reward delete type.
	 *
	 * @param itemObjectId 物品对象 ID / item object id
	 */
	public SM_DELETE_ITEM(int itemObjectId) {
		this(itemObjectId, ItemDeleteType.QUEST_REWARD);
	}

	/**
	 * 按对象 ID 与删除类型构造物品删除包。
	 * Creates an item deletion packet for the given object id and delete type.
	 *
	 * @param itemObjectId 物品对象 ID / item object id
	 * @param deleteType 删除类型（决定客户端动画与提示）/ delete type (drives client animation and notices)
	 */
	public SM_DELETE_ITEM(int itemObjectId, ItemDeleteType deleteType) {
		this.itemObjectId = itemObjectId;
		this.deleteType = deleteType;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(itemObjectId);
		writeC(deleteType.getMask());
	}
}
