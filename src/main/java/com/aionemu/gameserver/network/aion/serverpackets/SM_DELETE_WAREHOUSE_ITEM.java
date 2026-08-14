package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemDeleteType;

/**
 * 删除仓库物品包：按仓库类型、对象 ID 与删除类型移除。
 * Server packet that deletes a warehouse item by storage type, object id and delete type.
 *
 * @author kosyachok
 */
public class SM_DELETE_WAREHOUSE_ITEM extends AionServerPacket {

	private int warehouseType;
	private int itemObjId;
	private ItemDeleteType deleteType;

	/**
	 * 按仓库类型、对象 ID 与删除类型构造删除包。
	 * Creates a warehouse item deletion packet.
	 *
	 * @param warehouseType 仓库类型 / warehouse type
	 * @param itemObjId 物品对象 ID / item object id
	 * @param deleteType 删除类型 / delete type
	 */
	public SM_DELETE_WAREHOUSE_ITEM(int warehouseType, int itemObjId, ItemDeleteType deleteType) {
		this.warehouseType = warehouseType;
		this.itemObjId = itemObjId;
		this.deleteType = deleteType;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(warehouseType);
		writeD(itemObjId);
		writeC(deleteType.getMask());
	}
}
