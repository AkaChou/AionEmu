package com.aionemu.gameserver.services.item;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.controllers.observer.ItemUseObserver;
import com.aionemu.gameserver.dao.ItemStoneListDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ManaStone;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.stats.listeners.ItemEquipmentListener;
import com.aionemu.gameserver.model.templates.item.GodstoneInfo;
import com.aionemu.gameserver.model.templates.item.ItemCategory;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
/**
 * 物品镶嵌服务：魔石/融合石/神石镶嵌与移除，以及超限（amplification）。
 * Item socket service: manastone/fusion-stone/godstone socket and removal, plus amplification.
 */
@Slf4j
public class ItemSocketService {

	/**
	 * 为物品自动分配槽位镶嵌魔石。
	 * Sockets a manastone onto the item using the next free slot.
	 *
	 * @param item 目标物品 / target item
	 * manastone template id
	 * @return 新魔石，失败则为 {@code null} / new manastone, or {@code null} on failure
	 */
	public static ManaStone addManaStone(Item item, int itemId) {
		if (item == null) {
			return null;
		}
		Set<ManaStone> manaStones = item.getItemStones();
		if (manaStones.size() >= item.getSockets(false)) {
			return null;
		}
		ItemCategory manastoneCategory = DataManager.ITEM_DATA.getItemTemplate(itemId).getCategory();
		int specialSlotCount = item.getItemTemplate().getSpecialSlots();
		if (manastoneCategory == ItemCategory.SPECIAL_MANASTONE && specialSlotCount == 0) {
			return null;
		}
		int specialSlotsOccupied = 0;
		int normalSlotsOccupied = specialSlotCount;
		int maxSlot = specialSlotCount;
		HashSet<Integer> allSlots = new HashSet<>();
		for (ManaStone ms : manaStones) {
			ItemCategory category = DataManager.ITEM_DATA.getItemTemplate(ms.getItemId()).getCategory();
			if (category == ItemCategory.SPECIAL_MANASTONE) {
				specialSlotsOccupied++;
			}
			if (category == ItemCategory.MANASTONE) {
				normalSlotsOccupied++;
			}
			allSlots.add(ms.getSlot());
			if (maxSlot < ms.getSlot()) {
				maxSlot = ms.getSlot();
			}
		}
		if (specialSlotsOccupied >= specialSlotCount && manastoneCategory == ItemCategory.SPECIAL_MANASTONE) {
			return null;
		}
		int start = manastoneCategory == ItemCategory.SPECIAL_MANASTONE ? 0 : specialSlotCount;
		int end = manastoneCategory == ItemCategory.SPECIAL_MANASTONE ? specialSlotCount : manaStones.size();
		int nextSlot = start;
		boolean slotFound = false;
		for (; nextSlot < end; nextSlot++) {
			if (!allSlots.contains(nextSlot)) {
				slotFound = true;
				break;
			}
		}
		if (!slotFound) {
			if (specialSlotCount == 0 && manastoneCategory == ItemCategory.MANASTONE) {
				nextSlot = manaStones.size();
			}
			if (specialSlotCount > 0 && manastoneCategory == ItemCategory.SPECIAL_MANASTONE) {
				nextSlot = manaStones.size();
			}
			if (specialSlotCount > 0 && manastoneCategory == ItemCategory.MANASTONE) {
				nextSlot = normalSlotsOccupied;
			}
		}
		if (nextSlot >= item.getSockets(false)) {
			return null;
		}
		ManaStone stone = new ManaStone(item.getObjectId(), itemId, nextSlot, PersistentState.NEW);
		manaStones.add(stone);
		return stone;
	}

	/**
	 * 在指定槽位镶嵌魔石。
	 * Sockets a manastone into a fixed slot.
	 *
	 * @param item 目标物品 / target item
	 * manastone template id
	 * slot id
	 * @return 新魔石，失败则为 {@code null} / new manastone, or {@code null} on failure
	 */
	public static ManaStone addManaStone(Item item, int itemId, int slotId) {
		if (item == null) {
			return null;
		}
		Set<ManaStone> manaStones = item.getItemStones();
		if (manaStones.size() >= Item.MAX_BASIC_STONES) {
			return null;
		}
		ManaStone stone = new ManaStone(item.getObjectId(), itemId, slotId, PersistentState.NEW);
		manaStones.add(stone);
		return stone;
	}

	/**
	 * 复制源物品的魔石与融合石到目标物品。
	 * Copies manastones and fusion stones from source to target.
	 *
	 * source item
	 * target item
	 */
	public static void copyManaStones(Item source, Item target) {
		if (source.hasManaStones()) {
			for (ManaStone manaStone : source.getItemStones()) {
				target.getItemStones().add(new ManaStone(target.getObjectId(), manaStone.getItemId(),
						manaStone.getSlot(), PersistentState.NEW));
			}
			for (ManaStone manaStone : source.getFusionStones()) {
				target.getFusionStones().add(new ManaStone(target.getObjectId(), manaStone.getItemId(),
						manaStone.getSlot(), PersistentState.NEW));
			}
		}
	}

	/**
	 * 将源物品魔石复制为目标物品的融合石。
	 * Copies source manastones onto the target as fusion stones.
	 *
	 * source item
	 * target item
	 */
	public static void copyFusionStones(Item source, Item target) {
		if (source.hasManaStones()) {
			for (ManaStone manaStone : source.getItemStones()) {
				target.getFusionStones().add(new ManaStone(target.getObjectId(), manaStone.getItemId(),
						manaStone.getSlot(), PersistentState.NEW));
			}
		}
	}

	/**
	 * 为融合物品自动分配槽位镶嵌融合石并落库。
	 * Sockets a fusion stone onto a fused item (next free slot) and persists.
	 *
	 * @param item 目标物品 / target item
	 * @param itemId 融合石模板 ID / fusion-stone template id
	 * @return 新融合石，失败则为 {@code null} / new fusion stone, or {@code null} on failure
	 */
	public static ManaStone addFusionStone(Item item, int itemId) {
		if (item == null) {
			return null;
		}
		Set<ManaStone> manaStones = item.getFusionStones();
		if (manaStones.size() >= item.getSockets(true)) {
			return null;
		}
		ItemCategory manastoneCategory = DataManager.ITEM_DATA.getItemTemplate(itemId).getCategory();
		int specialSlotCount = item.getFusionedItemTemplate().getSpecialSlots();
		if (manastoneCategory == ItemCategory.SPECIAL_MANASTONE && specialSlotCount == 0) {
			return null;
		}
		int specialSlotsOccupied = 0;
		int normalSlotsOccupied = specialSlotCount;
		int maxSlot = specialSlotCount;
		HashSet<Integer> allSlots = new HashSet<>();
		for (ManaStone ms : manaStones) {
			ItemCategory category = DataManager.ITEM_DATA.getItemTemplate(ms.getItemId()).getCategory();
			if (category == ItemCategory.SPECIAL_MANASTONE) {
				specialSlotsOccupied++;
			}
			if (category == ItemCategory.MANASTONE) {
				normalSlotsOccupied++;
			}
			allSlots.add(ms.getSlot());
			if (maxSlot < ms.getSlot()) {
				maxSlot = ms.getSlot();
			}
		}
		if (specialSlotsOccupied >= specialSlotCount && manastoneCategory == ItemCategory.SPECIAL_MANASTONE) {
			return null;
		}
		int start = manastoneCategory == ItemCategory.SPECIAL_MANASTONE ? 0 : specialSlotCount;
		int end = manastoneCategory == ItemCategory.SPECIAL_MANASTONE ? specialSlotCount : manaStones.size();
		int nextSlot = start;
		boolean slotFound = false;
		for (; nextSlot < end; nextSlot++) {
			if (!allSlots.contains(nextSlot)) {
				slotFound = true;
				break;
			}
		}
		if (!slotFound) {
			if (specialSlotCount == 0 && manastoneCategory == ItemCategory.MANASTONE) {
				nextSlot = manaStones.size();
			}
			if (specialSlotCount > 0 && manastoneCategory == ItemCategory.SPECIAL_MANASTONE) {
				nextSlot = specialSlotsOccupied;
			}
			if (specialSlotCount > 0 && manastoneCategory == ItemCategory.MANASTONE) {
				nextSlot = normalSlotsOccupied;
			}
		}
		if (nextSlot >= item.getSockets(true)) {
			return null;
		}
		ManaStone stone = new ManaStone(item.getObjectId(), itemId, nextSlot, PersistentState.NEW);
		manaStones.add(stone);
		Set<ManaStone> itemStones = item.getFusionStones();
		DAOManager.getDAO(ItemStoneListDAO.class).storeFusionStones(itemStones);
		return stone;
	}

	/**
	 * 在指定槽位镶嵌融合石。
	 * Sockets a fusion stone into a fixed slot.
	 *
	 * @param item 目标物品 / target item
	 * @param itemId 融合石模板 ID / fusion-stone template id
	 * slot id
	 * @return 新融合石，失败则为 {@code null} / new fusion stone, or {@code null} on failure
	 */
	public static ManaStone addFusionStone(Item item, int itemId, int slotId) {
		if (item == null) {
			return null;
		}
		Set<ManaStone> fusionStones = item.getFusionStones();
		if (fusionStones.size() > item.getSockets(true)) {
			return null;
		}
		ManaStone stone = new ManaStone(item.getObjectId(), itemId, slotId, PersistentState.NEW);
		fusionStones.add(stone);
		return stone;
	}

	/**
	 * 移除玩家物品指定槽位的魔石（含已装备）。
	 * Removes a manastone at the given slot from inventory or equipped item.
	 *
	 * 玩家 / player
	 * item object id
	 * slot number
	 */
	public static void removeManastone(Player player, int itemObjId, int slotNum) {
		Storage inventory = player.getInventory();
		Item item = inventory.getItemByObjId(itemObjId);
		if (item == null) {
			item = player.getEquipment().getEquippedItemByObjId(itemObjId);
			if (item == null) {
				log.warn(I18n.get("log.e56ee8d1d462"));
				return;
			}
		}
		if (!item.hasManaStones()) {
			log.warn(I18n.get("log.e28c569e758a"));
			return;
		}
		Set<ManaStone> itemStones = item.getItemStones();
		int specialSlotCount = item.getItemTemplate().getSpecialSlots();
		for (ManaStone ms : itemStones) {
			if (item.isEquipped()) {
				ItemEquipmentListener.removeStoneStats1(item, ms, player.getGameStats());
			}

			if (ms.getSlot() == slotNum) {
				ms.setPersistentState(PersistentState.DELETED);
				DAOManager.getDAO(ItemStoneListDAO.class).storeManaStones(Collections.singleton(ms));
				itemStones.remove(ms);
				break;
			}
			if (ms.getSlot() > specialSlotCount) {
				ms.setPersistentState(PersistentState.DELETED);
				DAOManager.getDAO(ItemStoneListDAO.class).storeManaStones(Collections.singleton(ms));
				itemStones.remove(ms);
				break;
			}
			if (ms.getSlot() > slotNum && ms.getSlot() < specialSlotCount) {
				ms.setPersistentState(PersistentState.DELETED);
				DAOManager.getDAO(ItemStoneListDAO.class).storeManaStones(Collections.singleton(ms));
				itemStones.remove(ms);
				break;
			}
		}
		ItemPacketService.updateItemAfterInfoChange(player, item);
	}

	/**
	 * 移除玩家物品指定槽位的融合石（含已装备）。
	 * Removes a fusion stone at the given slot from inventory or equipped item.
	 *
	 * 玩家 / player
	 * item object id
	 * slot number
	 */
	public static void removeFusionstone(Player player, int itemObjId, int slotNum) {
		Storage inventory = player.getInventory();
		Item item = inventory.getItemByObjId(itemObjId);
		if (item == null) {
			item = player.getEquipment().getEquippedItemByObjId(itemObjId);
			if (item == null) {
				log.warn(I18n.get("log.e56ee8d1d462"));
				return;
			}
		}
		if (!item.hasFusionStones()) {
			log.warn(I18n.get("log.e28c569e758a"));
			return;
		}
		Set<ManaStone> itemStones = item.getFusionStones();
		int specialSlotCount = item.getFusionedItemTemplate().getSpecialSlots();
		for (ManaStone ms : itemStones) {
			if (item.isEquipped()) {
				ItemEquipmentListener.removeStoneStats1(item, ms, player.getGameStats());
			}
			if (ms.getSlot() == slotNum) {
				ms.setPersistentState(PersistentState.DELETED);
				DAOManager.getDAO(ItemStoneListDAO.class).storeFusionStones(Collections.singleton(ms));
				itemStones.remove(ms);
				break;
			}
			if (ms.getSlot() > specialSlotCount) {
				ms.setPersistentState(PersistentState.DELETED);
				DAOManager.getDAO(ItemStoneListDAO.class).storeFusionStones(Collections.singleton(ms));
				itemStones.remove(ms);
				break;
			}
			if (ms.getSlot() > slotNum && ms.getSlot() < specialSlotCount) {
				ms.setPersistentState(PersistentState.DELETED);
				DAOManager.getDAO(ItemStoneListDAO.class).storeFusionStones(Collections.singleton(ms));
				itemStones.remove(ms);
				break;
			}
		}
		ItemPacketService.updateItemAfterInfoChange(player, item);
	}

	/**
	 * 移除物品上全部魔石。
	 * Removes all manastones from the item.
	 *
	 * 玩家 / player
	 * item
	 */
	public static void removeAllManastone(Player player, Item item) {
		if (item == null) {
			log.warn(I18n.get("log.e56ee8d1d462"));
			return;
		}
		if (!item.hasManaStones()) {
			return;
		}
		Set<ManaStone> itemStones = item.getItemStones();
		for (ManaStone ms : itemStones) {
			ms.setPersistentState(PersistentState.DELETED);
		}
		DAOManager.getDAO(ItemStoneListDAO.class).storeManaStones(itemStones);
		itemStones.clear();
		ItemPacketService.updateItemAfterInfoChange(player, item);
	}

	/**
	 * 移除物品上全部融合石。
	 * Removes all fusion stones from the item.
	 *
	 * 玩家 / player
	 * item
	 */
	public static void removeAllFusionStone(Player player, Item item) {
		if (item == null) {
			log.warn(I18n.get("log.e56ee8d1d462"));
			return;
		}
		if (!item.hasFusionStones()) {
			return;
		}
		Set<ManaStone> fusionStones = item.getFusionStones();
		for (ManaStone ms : fusionStones) {
			ms.setPersistentState(PersistentState.DELETED);
		}
		DAOManager.getDAO(ItemStoneListDAO.class).storeFusionStones(fusionStones);
		fusionStones.clear();
		ItemPacketService.updateItemAfterInfoChange(player, item);
	}

	/**
	 * 将神石镶嵌到武器（扣基纳、播放使用动画）。
	 * Sockets a godstone onto a weapon (charges kinah and plays use animation).
	 *
	 * 玩家 / player
	 * weapon object id
	 * godstone object id
	 */
	public static void socketGodstone(final Player player, int weaponId, int stoneId) {
		final Item weaponItem = findGodstoneTarget(player, weaponId);
		final Item godstone = player.getInventory().getItemByObjId(stoneId);
		if (weaponItem == null) {
			PacketSendUtility.sendPacket(player,
					SM_SYSTEM_MESSAGE.STR_GIVE_ITEM_PROC_CANNOT_GIVE_PROC_TO_EQUIPPED_ITEM);
			return;
		}
		if (godstone == null) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_GIVE_ITEM_PROC_NO_PROC_GIVE_ITEM);
			return;
		}
		if (!weaponItem.canSocketGodstone()) {
			PacketSendUtility.sendPacket(player,
					SM_SYSTEM_MESSAGE.STR_GIVE_ITEM_PROC_NOT_ADD_PROC(new DescriptionId(weaponItem.getNameId())));
			return;
		}
		final int godStoneItemId = godstone.getItemTemplate().getTemplateId();
		if (player.getInventory().getKinah() < getPriceByQuality(godstone)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_MONEY);
			return;
		}
		ItemTemplate itemTemplate = DataManager.ITEM_DATA.getItemTemplate(godStoneItemId);
		GodstoneInfo godstoneInfo = itemTemplate.getGodstoneInfo();
		if (godstoneInfo == null) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_GIVE_ITEM_PROC_NO_PROC_GIVE_ITEM);
			log.warn(I18n.get("log.3d9472e37584", godStoneItemId));
			return;
		}
		if (!player.getInventory().decreaseByObjectId(stoneId, 1)) {
			return;
		}
		player.getInventory().decreaseKinah(getPriceByQuality(godstone));
		player.getController().cancelUseItem();
		PacketSendUtility.broadcastPacketAndReceive(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(),
				godstone.getObjectId(), godstone.getItemTemplate().getTemplateId(), 5000, 0, 0));
		final ItemUseObserver Enchant = new ItemUseObserver() {
			@Override
			public void abort() {
				if (player.getController().cancelTask(TaskId.ITEM_USE) == null) {
					player.getObserveController().removeObserver(this);
					return;
				}
				player.getObserveController().removeObserver(this);
				PacketSendUtility.sendPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId().intValue(),
						weaponItem.getObjectId().intValue(), weaponItem.getItemTemplate().getTemplateId(), 0, 3, 0));
				ItemPacketService.updateItemAfterInfoChange(player, weaponItem);
				PacketSendUtility.sendPacket(player,
						SM_SYSTEM_MESSAGE.STR_ENCHANT_ITEM_CANCELED(weaponItem.getItemTemplate().getNameId()));
			}
		};
		player.getObserveController().attach(Enchant);
		player.getController().scheduleTask(TaskId.ITEM_USE, new Runnable() {
			@Override
			public void run() {
				player.getController().cancelTask(TaskId.ITEM_USE);
				player.getObserveController().removeObserver(Enchant);
				weaponItem.addGodStone(godStoneItemId);
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE
						.STR_GIVE_ITEM_PROC_ENCHANTED_TARGET_ITEM(new DescriptionId(weaponItem.getNameId())));
				ItemPacketService.updateItemAfterInfoChange(player, weaponItem);
				PacketSendUtility.broadcastPacketAndReceive(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(),
						godstone.getObjectId(), godstone.getItemTemplate().getTemplateId(), 0, 1, 384));
			}
		}, 5000);
	}

	/**
	 * 在背包或装备栏中查找神石镶嵌目标武器。
	 * Finds the godstone target weapon in inventory or equipment.
	 *
	 * 玩家 / player
	 * weapon object id
	 * @return 武器物品或 {@code null} / weapon item or {@code null}
	 */
	static Item findGodstoneTarget(Player player, int weaponId) {
		Item weapon = player.getInventory().getItemByObjId(weaponId);
		return weapon != null ? weapon : player.getEquipment().getEquippedItemByObjId(weaponId);
	}

	/**
	 * 按神石品质返回镶嵌费用（基纳）。
	 * Returns godstone socket price (kinah) by item quality.
	 *
	 * godstone item
	 * price
	 */
	private static int getPriceByQuality(Item item) {
		int price = 0;
		switch (item.getItemTemplate().getItemQuality()) {
		case RARE:
			price = 842;
			break;
		case LEGEND:
			price = 2542;
			break;
		case UNIQUE:
			price = 7627;
			break;
		case EPIC:
			price = 22882;
			break;
		default:
			break;
		}
		return price;
	}

	/**
	 * 对物品执行超限（amplification）：消耗工具与强化石。
	 * Amplifies an item by consuming a tool and enchantment stone.
	 *
	 * 玩家 / player
	 * @param itemId 目标物品对象 ID / target item object id
	 * tool object id
	 * @param enchantmentStoneObjectId 强化石对象 ID / enchantment-stone object id
	 */
	@SuppressWarnings("null")
	public static void amplification(final Player player, int itemId, int toolUniqueId,
			final int enchantmentStoneObjectId) {
		final Item currentItem = player.getInventory().getItemByObjId(itemId);
		final Item toolItem = player.getInventory().getItemByObjId(toolUniqueId);
		final Item enchantStone = player.getInventory().getItemByObjId(enchantmentStoneObjectId);
		final int toolItemId = toolItem.getItemTemplate().getTemplateId();
		final int toolObjectId = toolItem.getObjectId();
		if (currentItem == null) {
			PacketSendUtility.sendPacket(player,
					SM_SYSTEM_MESSAGE.STR_MSG_EXCEED_NO_TARGET_ITEM(new DescriptionId(currentItem.getNameId())));
			return;
		}
		if (currentItem.isEquipped()) {
			PacketSendUtility.sendMessage(player, "Can't use on equiped item!");
			return;
		}
		if (currentItem.isPacked()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_EXCEED_CANNOT_03);
			return;
		}
		if (currentItem.isAmplified() && enchantStone.getItemTemplate().isEnchantmentStone()) {
			PacketSendUtility.sendPacket(player,
					SM_SYSTEM_MESSAGE.STR_MSG_EXCEED_ENCHANT_CANNOT_02(new DescriptionId(enchantStone.getNameId())));
			return;
		}
		if (currentItem != null & currentItem.isAmplified()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_EXCEED_ALREADY);
			return;
		}
		if (currentItem != null & currentItem.getEnchantLevel() < currentItem.getItemTemplate().getMaxEnchantLevel()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_EXCEED_CANNOT_02);
			return;
		}
		if (currentItem != null & !currentItem.canAmplification()) {
			PacketSendUtility.sendPacket(player,
					SM_SYSTEM_MESSAGE.STR_MSG_EXCEED_CANNOT_01(new DescriptionId(currentItem.getNameId())));
			return;
		}
		PacketSendUtility.broadcastPacket(player,
				new SM_ITEM_USAGE_ANIMATION(player.getObjectId().intValue(), toolObjectId, toolItemId, 5000, 0, 0),
				true);
		final ItemUseObserver observer = new ItemUseObserver() {
			@Override
			public void abort() {
				if (player.getController().cancelTask(TaskId.ITEM_USE) == null) {
					player.getObserveController().removeObserver(this);
					return;
				}
				player.removeItemCoolDown(toolItem.getItemTemplate().getUseLimits().getDelayId());
				PacketSendUtility.broadcastPacket(player,
						new SM_ITEM_USAGE_ANIMATION(player.getObjectId().intValue(), toolObjectId, toolItemId, 0, 2, 0),
						true);
				player.getObserveController().removeObserver(this);
			}
		};
		player.getObserveController().attach(observer);
		player.getController().scheduleTask(TaskId.ITEM_USE, new Runnable() {
			@Override
			public void run() {
				player.getObserveController().removeObserver(observer);
				PacketSendUtility.broadcastPacket(player,
						new SM_ITEM_USAGE_ANIMATION(player.getObjectId().intValue(), toolObjectId, toolItemId, 0, 1, 1),
						true);
				if (!player.getInventory().decreaseByObjectId(toolObjectId, 1))
					return;
				if (!player.getInventory().decreaseByObjectId(enchantmentStoneObjectId, 1))
					return;
				currentItem.setAmplification(true);
				player.getInventory().setPersistentState(PersistentState.UPDATE_REQUIRED);
				currentItem.setPersistentState(PersistentState.UPDATE_REQUIRED);
				ItemPacketService.updateItemAfterInfoChange(player, currentItem);
				PacketSendUtility.sendPacket(player,
						SM_SYSTEM_MESSAGE.STR_MSG_EXCEED_SUCCEED(new DescriptionId(currentItem.getNameId())));
			}
		}, 5000);
	}
}
