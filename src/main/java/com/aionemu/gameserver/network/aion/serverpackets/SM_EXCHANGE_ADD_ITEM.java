package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob;

/**
 * 交易添加物品包：通知己方/对方交易窗口新增物品及其完整信息。
 * Exchange add-item packet: notifies self/other trade window of a new item with full blob.
 *
 * @author Avol
 * @author ATracer
 */
public class SM_EXCHANGE_ADD_ITEM extends AionServerPacket {

	private Player player;
	private int action;
	private Item item;

	/**
	 * 0=self, 1=other。
	 * @param item   放入的物品 / item added
	 * @param player 所属玩家（用于写 ItemInfoBlob） / owner player for ItemInfoBlob
	 */
	public SM_EXCHANGE_ADD_ITEM(int action, Item item, Player player) {
		this.player = player;
		this.action = action;
		this.item = item;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		ItemTemplate itemTemplate = item.getItemTemplate();
		writeC(action); // 0 -self 1-other
		writeD(itemTemplate.getTemplateId());
		writeD(item.getObjectId());
		writeNameId(itemTemplate.getNameId());
		ItemInfoBlob itemInfoBlob = ItemInfoBlob.getFullBlob(player, item);
		itemInfoBlob.writeMe(getBuf());
	}
}
