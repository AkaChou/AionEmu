package com.aionemu.gameserver.network.aion.serverpackets;


import com.aionemu.boot.i18n.I18n;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.model.drop.Drop;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.DropNpc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 掉落物品列表的服务端包，向玩家展示可拾取物品。
 * Server packet that delivers the loot item list for a drop target.
 */
@Slf4j
public class SM_LOOT_ITEMLIST extends AionServerPacket {
	private int targetObjectId;
	private final boolean teamMembersNearby;
	private List<DropItem> dropItems;

	/**
	 * 构造玩家可见的掉落物品列表包。
	 * Builds the loot item list visible to the given player.
	 *
	 * drop NPC
	 * @param setItems 掉落物品集合 / set of drop items
	 * @param player 接收列表的玩家 / player receiving the list
	 */
	public SM_LOOT_ITEMLIST(DropNpc dropNpc, Set<DropItem> setItems, Player player) {
		this.targetObjectId = dropNpc.getObjectId();
		this.teamMembersNearby = dropNpc.getInRangePlayers().size() > 1 && dropNpc.getInRangePlayers().contains(player);
		this.dropItems = new ArrayList<>();
		if (setItems == null) {
			log.warn(I18n.get("log.4c7339d87f1f"));
			return;
		}
		for (DropItem item : setItems) {
			if (item.canViewDropItem(player.getObjectId())) {
				dropItems.add(item);
			}
		}
	}

	@Override
	protected void writeImpl(AionConnection con) {
		Player activePlayer = con.getActivePlayer();
		if (activePlayer == null) {
			return;
		}
		writeD(targetObjectId);
		writeC(dropItems.size());
		for (DropItem dropItem : dropItems) {
			Drop drop = dropItem.getDropTemplate();
			writeC(dropItem.getIndex());
			writeH(0);// unk 5.3
			writeC(0);// unk 5.3
			writeD(drop.getItemId());
			writeD((int) dropItem.getCount());
			writeC(dropItem.getOptionalSocket());
			writeC(0);
			writeC(0);
			ItemTemplate template = drop.getItemTemplate();
			boolean showLootConfirmation = !template.isTradeable();
			if (dropItem.isOnlyPossibleLooter(activePlayer) || !teamMembersNearby) {
				showLootConfirmation = false;
			}
			writeC(showLootConfirmation ? 1 : 0);
		}
	}
}
