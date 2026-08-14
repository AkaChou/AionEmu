package com.aionemu.gameserver.model.templates.item.actions;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.configs.administration.DeveloperConfig;
import com.aionemu.gameserver.configs.main.EnchantsConfig;
import com.aionemu.gameserver.controllers.observer.ItemUseObserver;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.templates.item.DisassembleItem;
import com.aionemu.gameserver.model.templates.item.DisassembleItemGroups;
import com.aionemu.gameserver.model.templates.item.DisassembleItems;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SELECT_ITEM;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SELECT_ITEM_ADD;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * Disassembly 动作模板（静态数据/XML）。
 * XML template.
 *
 * @author BeckUp.Media
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DisassemblyAction")
@Slf4j
public class DisassemblyAction extends AbstractItemAction
{
	// 模式 0：拆解；模式 1：拆解选择箱 / we have an mode 0 (disassembly) and an mode 1 (disassembly select box)
	@XmlAttribute(name = "mode")
	public int mode;

	// 可通过配置设置速度（3000 会出问题） / we can set the speed over config (3000 makes probs)
	private static final int USAGE_DELAY = 2000;

	// 可激活 WhatsInsideTheBox 方法 / we can activate the WhatsInsideTheBox methode
	private static final boolean DISASSEMBLY_DEBUG = true;

	/**
	 * 判断玩家是否可对该物品执行操作。
	 * ask if the Player can act with the Item.
	 */
	@Override
	public boolean canAct(Player player, Item parentItem, Item targetItem)
	{
		// 例如部分物品仅等级 >10 可打开 / some items you only can open with level > 10 as examle
		int usageLevel = parentItem.getItemTemplate().getRequiredLevel(player.getCommonData().getPlayerClass());
		if (usageLevel > player.getLevel()) {
			// 达到等级 %d 前无法使用 %s。 / You cannot use %s until you reach level %d.
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_USE_ITEM_TOO_LOW_LEVEL_MUST_BE_THIS_LEVEL(parentItem.getNameId(), parentItem.getItemTemplate().getLevel()));
			return false;
		}
		// 拆解内所有内容的 itemsCollections / the itemsCollections of all stuff inside the disassembly
		List<DisassembleItemGroups> itemsCollections = DataManager.DISASSEMBLY_ITEMS_DATA.getInfoByItemId(parentItem.getItemId());
		// 若物品为空，向玩家发送消息 / what if the item is empty, we send an msg to player
		if (itemsCollections == null || itemsCollections.isEmpty()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_DECOMPOSE_ITEM_INVALID_STANCE(parentItem.getNameId()));
			PacketSendUtility.sendMessage(player, "There is nothing inside, pls open a bug report for this Item: " + parentItem.getItemId());
			return false;
		}
		// 战斗中不可拆解（attackMode），偏硬核，后续可放宽。 / player cant use disassembly in combat, attackMode(), maybe a bit hardcore - we can remove it later
		if (player.getController().isInCombat() || player.isAttackMode()) {
			// 战斗中无法提取物品。 / You cannot extract item while in combat.
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_DECOMPOSE_ITEM_INVALID_STANCE(2800159));
			return false;
		}
		return true;
	}

	/**
	 * 开始物品动作。 / Start the ItemAction.
	 */
	@Override
	public void act(final Player player, final Item parentItem, final Item targetItem)
	{
		// 我们停止使用物品。 / we stop use an item.
		player.getController().cancelUseItem();
		// 获取含全部可能组的拆解物品。 / we get the disassembly item with all possible groups.
		List<DisassembleItemGroups> itemGroupCollection = DataManager.DISASSEMBLY_ITEMS_DATA.getInfoByItemId(parentItem.getItemId());
		// 排除最低/最高等级、种族与职业 / we exclude the min/max level, race and playerclass
		List<DisassembleItemGroups> itemGroupCollectionFiltered = filterGroupsByLevelRaceClass(player, itemGroupCollection);
		// 计算每组几率 / now lets calc the chance for each group
		List<DisassembleItemGroups> finalGroupCollection = calculateGroupChance(itemGroupCollectionFiltered, player, parentItem);
		// 计算物品几率 / lets calculate the chance for the items
		final List<DisassembleItem> finalItemCollection;
		// 选择箱不需要 itemUseObserver，5.8 由 C_SELECT_ITEM 即时处理。 / we dont need an itemUseObserver on SelectBox, comes from C_SELECT_ITEM in 5.8 instant
		if (this.mode == 1) { // SelectBox
			// 计算概率（isSelect == true 时不计算，直接添加全部物品）。 / calc the chance (isSelect == true - we dont need to calc, we simply add all items here)
			finalItemCollection = calculateItemchance(player, finalGroupCollection, true);
			// 清空列表（安全机制） / we clear the List (safety mechanic)
			if (player.getDisassemblyItemLists().size() > 0)
				player.getDisassemblyItemLists().clear();
			// 为该玩家设置列表 / we set the List for this Player
			player.setDisassemblyItemLists(finalItemCollection);
			// 发送含全部可选物品的 S 包 / we send the S packet with all Selectable Items
			PacketSendUtility.sendPacket(player, new SM_SELECT_ITEM(finalItemCollection, parentItem.getObjectId().intValue()));
			return;
		} else { // Normal DisassemblyBox
			// 发送 S_USE_ITEM 使用动画包。 / send the S_USE_ITEM packet
			PacketSendUtility.broadcastPacketAndReceive(player,
					new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), parentItem.getObjectId(), parentItem.getItemId(), USAGE_DELAY,
							0, 0));
			// 调试：最大值未达 10000 时发送消息 / debug stuff here, it sends an msg if the max value not reach 10000
			if (DISASSEMBLY_DEBUG)
				checkWhatsInsideTheBox(itemGroupCollection, player, parentItem);
			// 我们计算几率 / we calc the chance
			finalItemCollection = calculateItemchance(player, finalGroupCollection, false);
			// 观察者 / observer
			final ItemUseObserver observer = new ItemUseObserver()
			{
				// 玩家中止动作 / player abort the action
				/** 中止 / abort. */
				@Override
				public void abort()
				{
					player.getController().cancelTask(TaskId.ITEM_USE);
					player.removeItemCoolDown(parentItem.getItemTemplate().getUseLimits().getDelayId());
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ITEM_CANCELED(new DescriptionId(parentItem.getItemTemplate().getNameId())));
					PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(),
							parentItem.getObjectId(), parentItem.getItemTemplate().getTemplateId(), 0, 2, 0), true);
					player.getObserveController().removeObserver(this);
				}
			};
			player.getObserveController().attach(observer);
			player.getController().addTask(TaskId.ITEM_USE, GameThreadPoolServices.threadPoolManager().schedule(new Runnable()
			{
				// 玩家提交动作 / player submit the action
				/** 运行 / run. */
				@Override
				public void run()
				{
					player.getObserveController().removeObserver(observer);
					// 检查动作是否有效 / we check if the action is valid
					boolean isValidAction = checkValidate(player, parentItem);
					if (isValidAction) {
						if (finalItemCollection.size() > 0) {
							for (DisassembleItem item : finalItemCollection) {
								ItemService.addItem(player, item.getItemId(), item.getCount());
							}
							PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_DECOMPOSE_ITEM_SUCCEED(parentItem.getNameId()));
						}
					}
					PacketSendUtility.broadcastPacketAndReceive(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(),
							parentItem.getObjectId(), parentItem.getItemId(), 0, isValidAction ? 1 : 2, 0));
					player.getController().cancelTask(TaskId.ITEM_USE);
				}

				/**
	 * 校验操作、计算背包空位并移除待拆解物品。
	 * Validates the action, calculates free inventory slots and removes the source item.
	 *
	 * @param player
	 * @param parentItem
	 * @return
	 */
				boolean checkValidate(Player player, Item parentItem)
				{
					if (!canAct(player, parentItem, targetItem)) {
						return false;
					}
					Storage playerInventory = player.getInventory();
					int invSlotReq = calcUsedSlotsFromAction(finalItemCollection, false);
					int specialSlotreq = calcUsedSlotsFromAction(finalItemCollection, true);
					if (invSlotReq > 0 && playerInventory.getFreeSlots() < invSlotReq) {
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_DECOMPRESS_INVENTORY_IS_FULL);
						return false;
					}
					if (specialSlotreq > 0 && playerInventory.getSpecialCubeFreeSlots() < specialSlotreq) {
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_DECOMPRESS_INVENTORY_IS_FULL);
						return false;
					}
					if (player.getLifeStats().isAlreadyDead() || !player.isSpawned()) {
						return false;
					}
					if (!player.getInventory().decreaseByObjectId(parentItem.getObjectId(), 1)) {
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_DECOMPOSE_ITEM_NO_TARGET_ITEM);
						return false;
					}
					if (finalItemCollection.isEmpty()) {
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_DECOMPOSE_ITEM_FAILED(parentItem.getNameId()));
						return false;
					}
					return true;
				}
			}, USAGE_DELAY));
		}
	}

	/**
	 * 计算打开该物品所需槽位数。 / We need to know how many slots it will take to open the item.
	 */
	private int calcUsedSlotsFromAction(List<DisassembleItem> finalItemCol, boolean specialCube)
	{
		int maxCount = 0;
		for (DisassembleItem item : finalItemCol) {
			ItemTemplate template = DataManager.ITEM_DATA.getItemTemplate(item.getItemId());
			if (specialCube && template.getExtraInventoryId() > 0) {
				maxCount++;
			} else if (template.getExtraInventoryId() < 1) {
				maxCount++;
			}
		}
		return maxCount;
	}

	/**
	 * 计算物品几率；选择箱则直接加入全部物品。 / We calc the itemChance, for SelectBox we simply add all items.
	 */
	private List<DisassembleItem> calculateItemchance(Player player, List<DisassembleItemGroups> finalGroups, boolean isSelect)
	{
		List<DisassembleItem> newItemCollection = new ArrayList<DisassembleItem>();
		if (isSelect) {
			for (DisassembleItemGroups group : finalGroups) {
				List<DisassembleItems> itemList = group.getGroupItems();
				for (DisassembleItems item : itemList) {
					newItemCollection.add(item.getItem());
				}
			}
			return newItemCollection;
		} else {
			for (DisassembleItemGroups group : finalGroups) {
				List<DisassembleItems> itemList = group.getGroupItems();
				int currentSum = 0;
				int rnd;
				if (itemList.size() == 1 && itemList.get(0).getItemProb() == 10000)
					rnd = 1;
				else
					rnd = Rnd.get(0, 10000);
				for (DisassembleItems item : itemList) {
					currentSum += item.getItemProb();
					if (rnd < currentSum) {
						if (player.isGM())
							PacketSendUtility.sendMessage(player, String.format("Disassembly rnd: %d - hit current value: %d", rnd, currentSum));
						newItemCollection.add(item.getItem());
						break;
					}
				}
			}
			return newItemCollection;
		}
	}

	/**
	 * 检查数值是否高于或低于 10000。 / I use this now to check if an Value is higher or lower as 10000.
	 */
	private void checkWhatsInsideTheBox(List<DisassembleItemGroups> AllGroups, Player player, Item parentItem)
	{
		int index = 0;
		for (DisassembleItemGroups group : AllGroups) {
			List<DisassembleItems> itemList = group.getGroupItems();
			int probMax = 0;
			for (DisassembleItems item : itemList) {
				probMax += item.getItemProb();
			}
			if (probMax < 10000) {
				PacketSendUtility.sendMessage(player, String.format(
						"Pls do a report. Something is wrong on %d - The Max Value is under 10000 on index %d with only a maxValue from %d"
						, parentItem.getItemId(), index, probMax));
				log.error(I18n.get("log.9b31684a19c4", parentItem.getItemId(), index, probMax));
			} else if (probMax > 10000) {
				PacketSendUtility.sendMessage(player, String.format(
						"Pls do a report. Something is wrong on %d - The Max Value is above 10000 on index %d with only a maxValue from %d"
						, parentItem.getItemId(), index, probMax));
				log.error(I18n.get("log.1c7526a2fc2e", parentItem.getItemId(), index, probMax));
			}
			index++;
		}
	}

	/**
	 * 内部有数值低于 1000 的组，需计算玩家获得多少组。 / we have groups inside with a value below 1000, so we need to calc how many groups the player get.
	 */
	private List<DisassembleItemGroups> calculateGroupChance(List<DisassembleItemGroups> filteredList, Player player, Item parentitem)
	{
		List<DisassembleItemGroups> newCollection = new ArrayList<DisassembleItemGroups>();
		for (DisassembleItemGroups group : filteredList) {
			int rnd = Rnd.get(0, 1000);
			if (rnd < group.getGroupProb())
				newCollection.add(group);
			if (group.getGroupProb() == -1 && this.mode == 0) {
				PacketSendUtility.sendMessage(player, String.format(
						"Something is wrong on %d", parentitem.getItemId()));
			}
		}
		return newCollection;
	}

	/**
	 * 按等级、种族与职业过滤，排除不符的组。 / We need to check the level, race and playerclass and excludet the wrong groups.
	 */
	private List<DisassembleItemGroups> filterGroupsByLevelRaceClass(Player player, List<DisassembleItemGroups> collection)
	{
		int playerLevel = player.getLevel();
		Race playerRace = player.getRace();
		PlayerClass pClass = player.getPlayerClass();
		List<DisassembleItemGroups> newCollection = new ArrayList<DisassembleItemGroups>();
		for (DisassembleItemGroups group : collection) {
			if (group.getRace() != Race.PC_ALL && group.getRace() != playerRace)
				continue;
			if (group.getPlayerClassList() != null && !group.getPlayerClassList().contains(pClass))
				continue;
			if (group.getMinLevel() > playerLevel)
				continue;
			if (group.getMaxLevel() > 0 && group.getMaxLevel() < playerLevel)
				continue;
			newCollection.add(group);
		}
		return newCollection;
	}
}
