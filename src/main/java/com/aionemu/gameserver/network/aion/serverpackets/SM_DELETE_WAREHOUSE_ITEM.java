package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemDeleteType;
import lombok.AllArgsConstructor;

/**
 * 删除仓库物品包：按仓库类型、对象 ID 与删除类型移除。
 * Server packet that deletes a warehouse item by storage type, object id and delete type.
 *
 * @author kosyachok
 */
@AllArgsConstructor
public class SM_DELETE_WAREHOUSE_ITEM extends AionServerPacket {

	private final int warehouseType;
	private final int itemObjId;
	private final ItemDeleteType deleteType;

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(warehouseType);
		writeD(itemObjId);
		writeC(deleteType.getMask());
	}
}
