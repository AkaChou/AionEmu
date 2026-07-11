package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import java.util.Collection;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob;
import com.aionemu.gameserver.services.RepurchaseService;

/**
 * 向客户端同步可回购物品列表（NPC 回购窗口）。
 * Server packet synchronizing the repurchase item list for an NPC buyback window.
 *
 * @author xTz, KID
 */
public class SM_REPURCHASE extends AionServerPacket {

	private Player player;
	private final int targetObjectId;
	private final Collection<Item> items;

	/**
	 * 使用给定参数构造 SM_REPURCHASE 包。
	 * Creates a SM_REPURCHASE packet with the given parameters.
	 *
	 * 玩家 / player
	 * npc id
	 */
	public SM_REPURCHASE(Player player, int npcId) {
		this.player = player;
		this.targetObjectId = npcId;
		items = GameFeatureServices.repurchaseService().getRepurchaseItems(player.getObjectId());
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(targetObjectId);
		writeD(1);
		writeH(items.size());

		for (Item item : items) {
			ItemTemplate itemTemplate = item.getItemTemplate();
			writeD(item.getObjectId());
			writeD(itemTemplate.getTemplateId());
			writeNameId(itemTemplate.getNameId());
			ItemInfoBlob itemInfoBlob = ItemInfoBlob.getFullBlob(player, item);
			itemInfoBlob.writeMe(getBuf());
			writeQ(item.getRepurchasePrice());
		}
	}
}
