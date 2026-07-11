package com.aionemu.gameserver.network.aion.clientpackets;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.item.ItemUpgradeService;

/**
 * 客户端物品精炼/升级（Purification）请求包。
 * Client packet for item purification/upgrade.
 *
 * @author Ranastic (Encom)
 */
@Slf4j

public class CM_PURIFICATION_ITEM extends AionClientPacket {
	int playerObjectId;
	int upgradedItemObjectId;
	int resultItemId;
	int requireItemObjectId1;
	int requireItemObjectId2;
	int requireItemObjectId3;
	int requireItemObjectId4;
	int requireItemObjectId5;
	Item baseItem;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_PURIFICATION_ITEM(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		Player player = getConnection().getActivePlayer();
		playerObjectId = readD();
		upgradedItemObjectId = readD();
		resultItemId = readD();
		requireItemObjectId1 = readD();
		requireItemObjectId2 = readD();
		requireItemObjectId3 = readD();
		requireItemObjectId4 = readD();
		requireItemObjectId5 = readD();
		Storage inventory = player.getInventory();
		baseItem = inventory.getItemByObjId(upgradedItemObjectId);
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player == null) {
			return;
		}
		if (!ItemUpgradeService.checkItemUpgrade(player, baseItem, resultItemId)) {
			return;
		}
		Item resultItem = ItemService.newUpgradeItem(resultItemId);
		ItemService.makeUpgradeItem(baseItem, resultItem);
		if (!ItemUpgradeService.decreaseMaterial(player, baseItem, resultItemId)) {
			return;
		}
		ItemService.addItem(player, resultItem);
	}
}
