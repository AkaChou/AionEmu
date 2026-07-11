package com.aionemu.gameserver.services;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemQuality;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.item.ItemPacketService;
import com.aionemu.gameserver.services.item.ItemSocketService;
import com.aionemu.gameserver.services.trade.PricesService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import lombok.extern.slf4j.Slf4j;

/**
 * 武器融合服务，处理双手武器合成与拆解。
 * Arms fusion service handling two-handed weapon compound and decompound.
 */
@Slf4j
public class ArmsfusionService {

	/**
	 * 将副武器属性融合到主武器上，并消耗副武器与基纳。
	 * Fuses the secondary weapon into the primary weapon, consuming the secondary item and kinah.
	 *
	 * @param player 玩家 / player
	 * @param firstItemUniqueId 主武器唯一 ID / primary weapon object id
	 * @param secondItemUniqueId 副武器唯一 ID / secondary weapon object id
	 */
	public static void fusionWeapons(Player player, int firstItemUniqueId, int secondItemUniqueId) {
		Item firstItem = player.getInventory().getItemByObjId(firstItemUniqueId);
		if (firstItem == null) {
			firstItem = player.getEquipment().getEquippedItemByObjId(firstItemUniqueId);
		}
		Item secondItem = player.getInventory().getItemByObjId(secondItemUniqueId);
		if (secondItem == null) {
			secondItem = player.getEquipment().getEquippedItemByObjId(secondItemUniqueId);
		}
		if (firstItem == null || secondItem == null) {
			return;
		}
		double priceRate = PricesService.getGlobalPrices(player.getRace()) * .01;
		double taxRate = PricesService.getTaxes(player.getRace()) * .01;
		double rarity = rarityRate(firstItem.getItemTemplate().getItemQuality());
		int priceMod = PricesService.getGlobalPricesModifier() * 2;
		int level = firstItem.getItemTemplate().getLevel();
		int price = (int) (priceMod * priceRate * taxRate * rarity * level * level);
		if (player.getInventory().getKinah() < price) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE
					.STR_COMPOUND_ERROR_NOT_ENOUGH_MONEY(firstItem.getNameId(), secondItem.getNameId()));
			return;
		}
		if (firstItem.hasFusionedItem()) {
			PacketSendUtility.sendPacket(player,
					SM_SYSTEM_MESSAGE.STR_COMPOUND_ERROR_NOT_AVAILABLE(firstItem.getNameId()));
			return;
		}
		if (secondItem.hasFusionedItem()) {
			PacketSendUtility.sendPacket(player,
					SM_SYSTEM_MESSAGE.STR_COMPOUND_ERROR_NOT_AVAILABLE(secondItem.getNameId()));
			return;
		}
		if (!firstItem.getItemTemplate().isCanFuse() || !secondItem.getItemTemplate().isCanFuse()) {
			PacketSendUtility.sendMessage(player, "You performed illegal operation, admin will catch you");
			log.info(I18n.get("log.f034ccd4897a", player.getName()));
			return;
		}
		if (!firstItem.getItemTemplate().isTwoHandWeapon()) {
			PacketSendUtility.sendPacket(player,
					SM_SYSTEM_MESSAGE.STR_COMPOUND_ERROR_NOT_AVAILABLE(firstItem.getNameId()));
			return;
		}
		if (firstItem.getItemTemplate().getWeaponType() != secondItem.getItemTemplate().getWeaponType()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_COMPOUND_ERROR_DIFFERENT_TYPE);
			return;
		}
		if (secondItem.getItemTemplate().getLevel() > firstItem.getItemTemplate().getLevel()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_COMPOUND_ERROR_MAIN_REQUIRE_HIGHER_LEVEL);
			return;
		}
		if (firstItem.getImprovement() != null && secondItem.getImprovement() != null) {
			if (firstItem.getImprovement().getChargeWay() != secondItem.getImprovement().getChargeWay()) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_COMPOUND_ERROR_NOT_COMPARABLE_ITEM);
				return;
			}
		}
		firstItem.setFusionedItem(secondItem.getItemTemplate());
		ItemSocketService.removeAllFusionStone(player, firstItem);
		if (secondItem.hasOptionalSocket()) {
			firstItem.setOptionalFusionSocket(secondItem.getOptionalSocket());
		} else {
			firstItem.setOptionalFusionSocket(0);
		}
		ItemSocketService.copyFusionStones(secondItem, firstItem);
		firstItem.setPersistentState(PersistentState.UPDATE_REQUIRED);
		DAOManager.getDAO(InventoryDAO.class).store(firstItem, player);
		if (!player.getInventory().decreaseByObjectId(secondItemUniqueId, 1))
			return;
		ItemPacketService.updateItemAfterInfoChange(player, firstItem);
		player.getInventory().decreaseKinah(price);
		PacketSendUtility.sendPacket(player,
				SM_SYSTEM_MESSAGE.STR_COMPOUND_SUCCESS(firstItem.getNameId(), secondItem.getNameId()));
	}

	/**
	 * 按品质返回融合价格倍率。
	 * Returns the fusion price multiplier for the given item quality.
	 *
	 * item quality
	 * price rate
	 */
	private static double rarityRate(ItemQuality rarity) {
		switch (rarity) {
		case COMMON:
			return 1.0;
		case RARE:
			return 1.25;
		case LEGEND:
			return 1.5;
		case UNIQUE:
			return 2.0;
		case EPIC:
			return 2.5;
		case MYTHIC:
			return 3.0;
		default:
			return 1.0;
		}
	}

	/**
	 * 拆解已融合武器，清除融合属性与融合石。
	 * Breaks a fused weapon, clearing fusion data and fusion stones.
	 *
	 * @param player 玩家 / player
	 * @param weaponToBreakUniqueId 待拆解武器唯一 ID / weapon object id to break
	 */
	public static void breakWeapons(Player player, int weaponToBreakUniqueId) {
		Item weaponToBreak = player.getInventory().getItemByObjId(weaponToBreakUniqueId);
		if (weaponToBreak == null) {
			weaponToBreak = player.getEquipment().getEquippedItemByObjId(weaponToBreakUniqueId);
		}
		if (weaponToBreak == null) {
			return;
		}
		if (!weaponToBreak.hasFusionedItem()) {
			PacketSendUtility.sendPacket(player,
					SM_SYSTEM_MESSAGE.STR_DECOMPOUND_ERROR_NOT_AVAILABLE(weaponToBreak.getNameId()));
			return;
		}
		weaponToBreak.setFusionedItem(null);
		ItemSocketService.removeAllFusionStone(player, weaponToBreak);
		DAOManager.getDAO(InventoryDAO.class).store(weaponToBreak, player);
		ItemPacketService.updateItemAfterInfoChange(player, weaponToBreak);
		PacketSendUtility.sendPacket(player,
				SM_SYSTEM_MESSAGE.STR_COMPOUNDED_ITEM_DECOMPOUND_SUCCESS(weaponToBreak.getNameId()));
	}
}
