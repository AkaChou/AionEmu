package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import java.util.List;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.templates.WarehouseExpandTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_INFO;
import com.aionemu.gameserver.utils.PacketSendUtility;
import lombok.extern.slf4j.Slf4j;

/**
 * 仓库服务，处理个人仓库扩展与仓库信息下发。
 * Warehouse service handling personal warehouse expansion and warehouse info delivery.
 */
@Slf4j
public class WarehouseService {

	private static final int MIN_EXPAND = 0;
	private static final int MAX_EXPAND = 11;

	/**
	 * 通过 NPC 发起仓库扩容请求（弹窗确认并扣费）。
	 * Initiates warehouse expansion via NPC (confirmation dialog and fee deduction).
	 *
	 * 玩家 / player
	 * expansion NPC
	 */
	public static void expandWarehouse(final Player player, Npc npc) {
		final WarehouseExpandTemplate expandTemplate = DataManager.WAREHOUSEEXPANDER_DATA
				.getWarehouseExpandListTemplate(npc.getNpcId());
		if (expandTemplate == null) {
			log.error(I18n.get("log.ccd0b4fe4d46", npc.getObjectTemplate().getTemplateId()));
			return;
		}
		if (npcCanExpandLevel(expandTemplate, player.getWarehouseSize() + 1)
				&& validateNewSize(player.getWarehouseSize() + 1)) {
			if (validateNewSize(player.getWarehouseSize() + 1)) {
				final int price = getPriceByLevel(expandTemplate, player.getWarehouseSize() + 1);
				RequestResponseHandler responseHandler = new RequestResponseHandler(npc) {
					@Override
					public void acceptRequest(Creature requester, Player responder) {
						if (player.getInventory().getKinah() < price) {
							PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300831));
							return;
						}
						expand(responder);
						player.getInventory().decreaseKinah(price);
					}

					@Override
					public void denyRequest(Creature requester, Player responder) {
					}
				};
				boolean result = player.getResponseRequester().putRequest(900686, responseHandler);
				if (result) {
					PacketSendUtility.sendPacket(player, new SM_QUESTION_WINDOW(900686, 0, 0, String.valueOf(price)));
				}
			}
		} else {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300432));
		}
	}

	/**
	 * 实际扩展玩家仓库容量一级。
	 * Actually expands the player's warehouse capacity by one level.
	 *
	 * @param player 玩家 / player
	 */
	public static void expand(Player player) {
		if (!canExpand(player)) {
			return;
		}
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300433, "8"));
		player.setWarehouseSize(player.getWarehouseSize() + 1);
		sendWarehouseInfo(player, false);
	}

	/**
	 * 校验目标扩容等级是否在合法区间内。
	 * Validates whether the target expand level is within the allowed range.
	 *
	 * @param level 目标等级 / target level
	 * whether valid
	 */
	private static boolean validateNewSize(int level) {
		if (level < MIN_EXPAND || level > MAX_EXPAND) {
			return false;
		}
		return true;
	}

	/**
	 * 判断玩家是否还能继续扩容。
	 * Checks whether the player can still expand the warehouse.
	 *
	 * @param player 玩家 / player
	 * @return 是否可扩容 / whether expansion is allowed
	 */
	public static boolean canExpand(Player player) {
		return validateNewSize(player.getWarehouseSize() + 1);
	}

	/**
	 * 判断该 NPC 模板是否支持指定扩容等级。
	 * Checks whether the NPC expand template supports the given level.
	 *
	 * @param clist 扩容模板 / expand template
	 * @param level 目标等级 / target level
	 * whether supported
	 */
	private static boolean npcCanExpandLevel(WarehouseExpandTemplate clist, int level) {
		if (!clist.contains(level)) {
			return false;
		}
		return true;
	}

	/**
	 * 按扩容等级获取价格。
	 * Returns the price for the given expand level.
	 *
	 * @param clist 扩容模板 / expand template
	 * @param level 目标等级 / target level
	 * price
	 */
	private static int getPriceByLevel(WarehouseExpandTemplate clist, int level) {
		return clist.get(level).getPrice();
	}

	/**
	 * 向客户端发送仓库（及可选账号仓库）物品信息。
	 * Sends warehouse (and optionally account warehouse) item info to the client.
	 *
	 * @param player 玩家 / player
	 * @param sendAccountWh 是否发送账号仓库 / whether to send account warehouse
	 */
	public static void sendWarehouseInfo(Player player, boolean sendAccountWh) {
		List<Item> items = player.getStorage(StorageType.REGULAR_WAREHOUSE.getId()).getItems();
		int whSize = player.getWarehouseSize();
		int itemsSize = items.size();
		boolean firstPacket = true;
		if (itemsSize != 0) {
			int index = 0;
			while (index + 10 < itemsSize) {
				PacketSendUtility.sendPacket(player, new SM_WAREHOUSE_INFO(items.subList(index, index + 10),
						StorageType.REGULAR_WAREHOUSE.getId(), whSize, firstPacket, player));
				index += 10;
				firstPacket = false;
			}
			PacketSendUtility.sendPacket(player, new SM_WAREHOUSE_INFO(items.subList(index, itemsSize),
					StorageType.REGULAR_WAREHOUSE.getId(), whSize, firstPacket, player));
		}
		PacketSendUtility.sendPacket(player,
				new SM_WAREHOUSE_INFO(null, StorageType.REGULAR_WAREHOUSE.getId(), whSize, false, player));
		if (sendAccountWh) {
			PacketSendUtility.sendPacket(player,
					new SM_WAREHOUSE_INFO(player.getStorage(StorageType.ACCOUNT_WAREHOUSE.getId()).getItemsWithKinah(),
							StorageType.ACCOUNT_WAREHOUSE.getId(), 0, true, player));
		}
		PacketSendUtility.sendPacket(player,
				new SM_WAREHOUSE_INFO(null, StorageType.ACCOUNT_WAREHOUSE.getId(), 0, false, player));
	}
}
