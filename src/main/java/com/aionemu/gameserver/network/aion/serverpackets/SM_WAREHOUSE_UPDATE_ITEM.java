package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob.ItemBlobType;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType;
import lombok.AllArgsConstructor;

/**
 * 更新仓库中单件物品信息的服务端包。
 * Server packet that updates a single warehouse item.
 *
 * @author kosyachok
 * @author -Nemesiss-
 */
@AllArgsConstructor
public class SM_WAREHOUSE_UPDATE_ITEM extends AionServerPacket {

	private final Player player;
	private final Item item;
	private final int warehouseType;
	private final ItemUpdateType updateType;

	@Override
	protected void writeImpl(AionConnection con) {
		ItemTemplate itemTemplate = item.getItemTemplate();
		writeD(item.getObjectId());
		writeC(warehouseType);
		writeNameId(itemTemplate.getNameId());
		ItemInfoBlob itemInfoBlob = new ItemInfoBlob(player, item);
		itemInfoBlob.addBlobEntry(ItemBlobType.GENERAL_INFO);
		itemInfoBlob.writeMe(getBuf());

		if (updateType.isSendable()) {
			writeH(updateType.getMask());
		}
	}
}
