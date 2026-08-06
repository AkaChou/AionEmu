package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

import com.aionemu.gameserver.configs.main.MembershipConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ItemSlot;
import com.aionemu.gameserver.model.skill.PlayerSkillEntry;
import com.aionemu.gameserver.model.templates.item.RequireSkill;
import com.aionemu.gameserver.model.templates.item.Stigma;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SKILL_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.skillengine.model.SkillLearnTemplate;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.audit.AuditLogger;

/**
 * 烙印（Stigma）服务，处理装备/卸下烙印、技能授予、套装强化与登录校验。
 * Stigma service handling equip/unequip, skill grants, set enchant bonuses, and login validation.
 *
 * @author Wnkrz (Encom)
 */
@Slf4j
public class StigmaService {

	/**
	 * 按品质返回烙印装备消耗基纳。
	 * Returns the kinah cost to equip a stigma by item quality.
	 *
	 * @param item 烙印道具 / stigma item
	 * price
	 */
	private static int getPriceByQuality(Item item) {
		int price = 0;
		switch (item.getItemTemplate().getItemQuality()) {
		case RARE:
			price = 35312;
			break;
		case LEGEND:
			price = 70625;
			break;
		case UNIQUE:
			price = 141250;
			break;
		default:
			break;
		}
		return price;
	}

	/**
	 * 装备烙印时校验槽位/职业/基纳，授予对应技能并检查连结与套装。
	 * On stigma equip, validates slot/class/kinah, grants skills, and checks linked skills and set bonuses.
	 *
	 * 玩家 / player
	 * @param resultItem 装备的烙印 / equipped stigma item
	 * @param slot 装备槽位 / equipment slot
	 * whether successful
	 */
	public static boolean notifyEquipAction(final Player player, Item resultItem, long slot) {
		if (resultItem.getItemTemplate().isStigma()) {
			if (ItemSlot.isRegularStigma(slot)) {
				if (getPossibleStigmaCount(player) <= player.getEquipment().getEquippedItemsRegularStigma().size()) {
					AuditLogger.info(player, "Possible client hack stigma count big :O");
					return false;
				}
			}
			if (!resultItem.getItemTemplate().isClassSpecific(player.getCommonData().getPlayerClass())) {
				AuditLogger.info(player, "Possible client hack not valid for class.");
				return false;
			}
			Stigma stigmaInfo = resultItem.getItemTemplate().getStigma();
			if (stigmaInfo == null) {
				log.warn(I18n.get("log.eb1508f439a1", resultItem.getItemTemplate().getTemplateId()));
				return false;
			}
			if (player.getInventory().getKinah() < getPriceByQuality(resultItem)) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_STIGMA_NOT_ENOUGH_MONEY);
				return false;
			} else {
				player.getInventory().decreaseKinah(getPriceByQuality(resultItem));
			}
			for (int i = 1; i <= player.getLevel(); i++) {
				SkillLearnTemplate[] skillTemplates = DataManager.SKILL_TREE_DATA
						.getTemplatesFor(player.getPlayerClass(), i, player.getRace());
				for (SkillLearnTemplate skillTree : skillTemplates) {
					if (resultItem.getSkillGroup().equals(skillTree.getSkillGroup())) {
						// PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300401, new
						// DescriptionId(resultItem.getNameId()), skillTree.getSkillLevel() +
						// resultItem.getEnchantLevel()));
						player.getSkillList().addStigmaSkill(player, skillTree.getSkillId(),
								skillTree.getSkillLevel() + resultItem.getEnchantLevel());
						PacketSendUtility.sendPacket(player,
								new SM_SKILL_LIST(player, player.getSkillList().getStigmaSkills()));
					}
				}
			}
			List<Integer> sStigma = player.getEquipment().getEquippedItemsAllStigmaIds();
			sStigma.add(resultItem.getItemId());
			StigmaLinkedService.checkEquipConditions(player, sStigma);
			checkStigmaEnchant(player, sStigma);
			if (player.getStigmaSet() != 0) {
				addStigmaSetEnchant(player, resultItem.getEnchantLevel());
			}
		}
		return true;
	}

	/**
	 * 卸下烙印时移除技能、连结技能与套装加成。
	 * On stigma unequip, removes skills, linked skills, and set bonuses as needed.
	 *
	 * @param player 玩家 / player
	 * @param resultItem 卸下的烙印 / unequipped stigma item
	 * @return 是否允许卸下 / whether unequip is allowed
	 */
	public static boolean notifyUnequipAction(Player player, Item resultItem) {
		return notifyUnequipAction(player, resultItem, true);
	}

	/** Applies unequip side effects without opening an independent stigma-list transaction. */
	public static boolean notifyUnequipActionInTransaction(Player player, Item resultItem) {
		return notifyUnequipAction(player, resultItem, false);
	}

	private static boolean notifyUnequipAction(Player player, Item resultItem, boolean storeStigmaListImmediately) {
		if (player.getEquipment().isSlotEquipped(ItemSlot.STIGMA_SPECIAL.getSlotIdMask())
				&& resultItem.getEquipmentSlot() != ItemSlot.STIGMA_SPECIAL.getSlotIdMask()) {
			return false;
		}
		if (player.getStigmaSet() != 0 && player.getEquipment().getEquippedItemsAllStigmaIds().size() == 6) {
			removeStigmaSetEnchant(player);
		}
		if (resultItem.getItemTemplate().isStigma()) {
			int itemId = resultItem.getItemId();
			PacketSendUtility.sendPacket(player,
					SM_CUBE_UPDATE.stigmaSlots(player.getCommonData().getAdvancedStigmaSlotSize()));
			PacketSendUtility.sendPacket(player, new SM_INVENTORY_UPDATE_ITEM(player, resultItem));
			for (int i = 1; i <= player.getLevel(); i++) {
				SkillLearnTemplate[] skillTemplates = DataManager.SKILL_TREE_DATA
						.getTemplatesFor(player.getPlayerClass(), i, player.getRace());
				for (SkillLearnTemplate skillTree : skillTemplates) {
					if (resultItem.getSkillGroup().equals(skillTree.getSkillGroup())) {
						// PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300401, new
						// DescriptionId(resultItem.getNameId()), resultItem.getEnchantLevel()));
						player.getSkillList().addStigmaSkill(player, skillTree.getSkillId(), skillTree.getSkillLevel());
						PacketSendUtility.sendPacket(player,
								new SM_SKILL_LIST(player, player.getSkillList().getStigmaSkills()));
						SkillLearnService.removeSkill(player, skillTree.getSkillId());
					}
				}
			}
			// PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300403, new
			// DescriptionId(resultItem.getNameId())));
			if (storeStigmaListImmediately) {
				player.getEquipedStigmaList().remove(player, itemId);
			} else {
				player.getEquipedStigmaList().removeInTransaction(itemId);
			}
			if (player.getEquipment().getEquippedItemsAllStigma().size() <= 6 && player.getLinkedSkill() != 0) {
				SkillTemplate linked = DataManager.SKILL_DATA.getSkillTemplate(player.getLinkedSkill());
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_STIGMA_DELETE_LINKED_SKILL(
						new DescriptionId(DataManager.SKILL_DATA.getSkillTemplate(linked.getSkillId()).getNameId()),
						1));
				StigmaLinkedService.DeleteLinkedSkills(player);
			}
			if (player.getEquipment().getEquippedItemsAllStigma().size() <= 6 && player.getStigmaSet() != 0) {
				player.setStigmaSet(0);
			}
		}
		return true;
	}

	/**
	 * 按套装等级为已有烙印技能叠加强化等级。
	 * Applies set-bonus enchant levels to existing stigma skills.
	 *
	 * @param player 玩家 / player
	 * @param enchantLevel 强化等级 / enchant level
	 */
	public static void addStigmaSetEnchant(Player player, int enchantLevel) {
		for (PlayerSkillEntry skill : player.getSkillList().getStigmaSkills()) {
			player.getSkillList().addStigmaSkill(player, skill.getSkillId(), 1 + enchantLevel + player.getStigmaSet());
			PacketSendUtility.sendPacket(player, new SM_SKILL_LIST(player, player.getSkillList().getStigmaSkills()));
		}
	}

	/**
	 * 强化连结技能相关烙印技能等级。
	 * Updates stigma skill levels for linked-skill enchant.
	 *
	 * @param player 玩家 / player
	 * @param enchantLevel 强化等级 / enchant level
	 */
	public static void enchanteLinkedSkill(Player player, int enchantLevel) {
		for (PlayerSkillEntry skill : player.getSkillList().getStigmaSkills()) {
			player.getSkillList().addStigmaSkill(player, skill.getSkillId(), 1 + enchantLevel + player.getStigmaSet());
			PacketSendUtility.sendPacket(player, new SM_SKILL_LIST(player, player.getSkillList().getStigmaSkills()));
		}
	}

	/**
	 * 移除套装加成后，按单件强化等级重建烙印技能。
	 * After set-bonus removal, rebuilds stigma skills from per-item enchant levels.
	 *
	 * @param player 玩家 / player
	 */
	public static void removeStigmaSetEnchant(Player player) {
		for (Item resultItem : player.getEquipment().getEquippedItemsAllStigma()) {
			for (int i = 1; i <= player.getLevel(); i++) {
				SkillLearnTemplate[] skillTemplates = DataManager.SKILL_TREE_DATA
						.getTemplatesFor(player.getPlayerClass(), i, player.getRace());
				for (SkillLearnTemplate skillTree : skillTemplates) {
					if (resultItem.getSkillGroup().equals(skillTree.getSkillGroup())) {
						player.getSkillList().addStigmaSkill(player, skillTree.getSkillId(),
								skillTree.getSkillLevel() + resultItem.getEnchantLevel());
						PacketSendUtility.sendPacket(player,
								new SM_SKILL_LIST(player, player.getSkillList().getStigmaSkills()));
					}
				}
			}
		}
	}

	/**
	 * 根据 6 件烙印强化等级设置套装加成值。
	 * Sets the stigma set bonus value from the enchant levels of 6 equipped stigmas.
	 *
	 * @param player 玩家 / player
	 * @param list 已装备烙印模板 ID 列表 / equipped stigma template ids
	 */
	public static void checkStigmaEnchant(Player player, List<Integer> list) {
		for (Item item : player.getEquipment().getEquippedItemsAllStigma()) {
			if (list.size() >= 6) {
				if (item.getEnchantLevel() == 6) {
					player.setStigmaSet(1);
				} else if (item.getEnchantLevel() == 7) {
					player.setStigmaSet(2);
				} else if (item.getEnchantLevel() == 8) {
					player.setStigmaSet(3);
				} else if (item.getEnchantLevel() == 9) {
					player.setStigmaSet(3);
				} else if (item.getEnchantLevel() >= 10) {
					player.setStigmaSet(5);
				} else {
					player.setStigmaSet(0);
				}
			}
		}
	}

	/**
	 * 登录时重建烙印技能、校验槽位/前置技能/职业，并检查连结条件。
	 * On login, rebuilds stigma skills, validates slots/prereqs/class, and checks linked conditions.
	 *
	 * 玩家 / player
	 */
	public static void onPlayerLogin(Player player) {
		List<Item> equippedItems = player.getEquipment().getEquippedItemsAllStigma();
		List<Integer> Stigma = player.getEquipment().getEquippedItemsAllStigmaIds();
		checkStigmaEnchant(player, Stigma);
		for (Item item : equippedItems) {
			for (int i = 1; i <= player.getLevel(); i++) {
				SkillLearnTemplate[] skillTemplates = DataManager.SKILL_TREE_DATA
						.getTemplatesFor(player.getPlayerClass(), i, player.getRace());
				for (SkillLearnTemplate skillTree : skillTemplates) {
					if (item.getItemTemplate().isStigma() && item.getSkillGroup().equals(skillTree.getSkillGroup())) {
						player.getSkillList().addStigmaSkill(player, skillTree.getSkillId(),
								skillTree.getSkillLevel() + item.getEnchantLevel() + player.getStigmaSet());
						PacketSendUtility.sendPacket(player,
								new SM_SKILL_LIST(player, player.getSkillList().getStigmaSkills()));
					}
				}
			}
		}
		for (Item item : equippedItems) {
			if (item.getItemTemplate().isStigma()) {
				if (!isPossibleEquippedStigma(player, item)) {
					AuditLogger.info(player, "Possible client hack stigma count big :O");
					player.getEquipment().unEquipItem(item.getObjectId(), 0);
					continue;
				}
				Stigma stigmaInfo = item.getItemTemplate().getStigma();
				if (stigmaInfo == null) {
					player.getEquipment().unEquipItem(item.getObjectId(), 0);
					continue;
				}
				int needSkill = stigmaInfo.getRequireSkill().size();
				for (RequireSkill rs : stigmaInfo.getRequireSkill()) {
					for (int id : rs.getSkillIds()) {
						if (player.getSkillList().isSkillPresent(id)) {
							needSkill--;
							break;
						}
					}
				}
				if (needSkill != 0) {
					AuditLogger.info(player, "Possible client hack advenced stigma skill.");
					player.getEquipment().unEquipItem(item.getObjectId(), 0);
					continue;
				}
				if (!item.getItemTemplate().isClassSpecific(player.getCommonData().getPlayerClass())) {
					AuditLogger.info(player, "Possible client hack not valid for class.");
					player.getEquipment().unEquipItem(item.getObjectId(), 0);
					continue;
				}
			}
		}
		/** 烙印关联技能 / Stigma Linked Skills */
		StigmaLinkedService.checkEquipConditions(player, Stigma);
	}

	/**
	 * 按等级、任务进度与会员权限计算可用常规烙印槽数量。
	 * Computes available regular stigma slot count from level, quest progress, and membership.
	 *
	 * 玩家 / player
	 * slot count
	 */
	private static int getPossibleStigmaCount(Player player) {
		if (player == null || player.getLevel() < 20) {
			return 0;
		}

		if (player.havePermission(MembershipConfig.STIGMA_SLOT_QUEST)) {
			return 7;
		}

		boolean isCompleteQuest = false;
		if (player.getRace() == Race.ELYOS) {
			isCompleteQuest = player.isCompleteQuest(1929)
					|| player.getQuestStateList().getQuestState(1929).getStatus() == QuestStatus.START
							&& player.getQuestStateList().getQuestState(1929).getQuestVars().getQuestVars() == 98;
		} else {
			isCompleteQuest = player.isCompleteQuest(2900)
					|| player.getQuestStateList().getQuestState(2900).getStatus() == QuestStatus.START
							&& player.getQuestStateList().getQuestState(2900).getQuestVars().getQuestVars() == 99;
		}
		int playerLevel = player.getLevel();
		if (isCompleteQuest) {
			if (playerLevel < 30) {
				return 2;
			} else if (playerLevel < 40) {
				return 3;
			} else if (playerLevel < 45) {
				return 4;
			} else if (playerLevel < 50) {
				return 5;
			} else if (playerLevel < 55) {
				return 6;
			} else if (playerLevel < 55 && player.getStigmaSet() >= 3) {
				return 7;
			} else {
				return 7;
			}
		}
		return 0;
	}

	/**
	 * 判断当前已装备烙印是否落在玩家可用槽位范围内。
	 * Returns whether the equipped stigma is within the player's available slot range.
	 *
	 * 玩家 / player
	 * @param item 烙印道具 / stigma item
	 * whether valid
	 */
	private static boolean isPossibleEquippedStigma(Player player, Item item) {
		if (player == null || item == null || !item.getItemTemplate().isStigma()) {
			return false;
		}
		long itemSlotToEquip = item.getEquipmentSlot();
		if (ItemSlot.isRegularStigma(itemSlotToEquip)) {
			int stigmaCount = getPossibleStigmaCount(player);
			if (stigmaCount > 0) {
				if (stigmaCount == 1) {
					if (itemSlotToEquip == ItemSlot.STIGMA1.getSlotIdMask()) {
						return true;
					}
				} else if (stigmaCount == 2) {
					if (itemSlotToEquip == ItemSlot.STIGMA1.getSlotIdMask()
							|| itemSlotToEquip == ItemSlot.STIGMA2.getSlotIdMask()) {
						return true;
					}
				} else if (stigmaCount == 3) {
					if (itemSlotToEquip == ItemSlot.STIGMA1.getSlotIdMask()
							|| itemSlotToEquip == ItemSlot.STIGMA2.getSlotIdMask()
							|| itemSlotToEquip == ItemSlot.STIGMA3.getSlotIdMask()) {
						return true;
					}
				} else if (stigmaCount == 4) {
					if (itemSlotToEquip == ItemSlot.STIGMA1.getSlotIdMask()
							|| itemSlotToEquip == ItemSlot.STIGMA2.getSlotIdMask()
							|| itemSlotToEquip == ItemSlot.STIGMA3.getSlotIdMask()
							|| itemSlotToEquip == ItemSlot.STIGMA4.getSlotIdMask()) {
						return true;
					}
				} else if (stigmaCount == 5) {
					if (itemSlotToEquip == ItemSlot.STIGMA1.getSlotIdMask()
							|| itemSlotToEquip == ItemSlot.STIGMA2.getSlotIdMask()
							|| itemSlotToEquip == ItemSlot.STIGMA3.getSlotIdMask()
							|| itemSlotToEquip == ItemSlot.STIGMA4.getSlotIdMask()
							|| itemSlotToEquip == ItemSlot.STIGMA5.getSlotIdMask()) {
						return true;
					}
				} else if (stigmaCount == 6) {
					if (itemSlotToEquip == ItemSlot.STIGMA1.getSlotIdMask()
							|| itemSlotToEquip == ItemSlot.STIGMA2.getSlotIdMask()
							|| itemSlotToEquip == ItemSlot.STIGMA3.getSlotIdMask()
							|| itemSlotToEquip == ItemSlot.STIGMA4.getSlotIdMask()
							|| itemSlotToEquip == ItemSlot.STIGMA5.getSlotIdMask()
							|| itemSlotToEquip == ItemSlot.STIGMA6.getSlotIdMask()) {
						return true;
					}
				} else if (stigmaCount == 7) {
					if (itemSlotToEquip == ItemSlot.STIGMA1.getSlotIdMask()
							|| itemSlotToEquip == ItemSlot.STIGMA2.getSlotIdMask()
							|| itemSlotToEquip == ItemSlot.STIGMA3.getSlotIdMask()
							|| itemSlotToEquip == ItemSlot.STIGMA4.getSlotIdMask()
							|| itemSlotToEquip == ItemSlot.STIGMA5.getSlotIdMask()
							|| itemSlotToEquip == ItemSlot.STIGMA6.getSlotIdMask()
							|| itemSlotToEquip == ItemSlot.STIGMA_SPECIAL.getSlotIdMask()) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
