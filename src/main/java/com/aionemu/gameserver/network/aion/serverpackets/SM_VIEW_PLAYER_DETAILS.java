package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.List;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob;

/**
 * 查看其他玩家装备详情的服务端包。
 * Server packet for viewing another player's equipment details.
 *
 * @author Avol, xTz
 */
public class SM_VIEW_PLAYER_DETAILS extends AionServerPacket {

	private List<Item> items;
	private int itemSize;
	private int targetObjId;
	private Player player;

	/**
	 * @param items  目标装备列表 / target equipment list
	 * @param player 被查看的玩家 / viewed player
	 */
	public SM_VIEW_PLAYER_DETAILS(List<Item> items, Player player) {
		this.player = player;
		this.targetObjId = player.getObjectId();
		this.items = items;
		this.itemSize = items.size();
	}

	@Override
	protected void writeImpl(AionConnection con) {

		writeD(targetObjId);
		writeC(11);
		writeH(itemSize);
		for (Item item : items) {
			writeItemInfo(item);
		}
	}

	/**
	 * 写出单件装备详情。
	 * Writes a single equipment item's details.
	 *
	 * @param item 装备物品 / equipment item
	 */
	private void writeItemInfo(Item item) {
		ItemTemplate template = item.getItemTemplate();
		writeD(0);
		writeD(template.getTemplateId());
		writeNameId(template.getNameId());
		ItemInfoBlob itemInfoBlob = ItemInfoBlob.getFullBlob(player, item);
		itemInfoBlob.writeMe(getBuf());
	}
}
