package com.aionemu.gameserver.services;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.configs.main.EnchantsConfig;
import com.aionemu.gameserver.controllers.observer.ItemUseObserver;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ItemSlot;
import com.aionemu.gameserver.model.items.ManaStone;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatEnchantFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatFunction;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.stats.listeners.ItemEquipmentListener;
import com.aionemu.gameserver.model.templates.item.ArmorType;
import com.aionemu.gameserver.model.templates.item.EnchantType;
import com.aionemu.gameserver.model.templates.item.ItemCategory;
import com.aionemu.gameserver.model.templates.item.ItemEnchantTemplate;
import com.aionemu.gameserver.model.templates.item.ItemQuality;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.item.actions.EnchantItemAction;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.item.ItemPacketService;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.item.ItemSocketService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.RndArray;
import com.aionemu.gameserver.utils.audit.AuditLogger;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 附魔服务：装备强化、拆解、魔力石镶嵌、手镯/装备词条应用，以及相关套装与技能判定。
 * Enchant service: equipment enchanting, dismantling, manastone socketing, bracelet/item modifier application, and related set/skill checks.
 *
 * @author Ranastic (Encom)
 */
@Slf4j
public class EnchantService {

	private static final int[] archDaevaStoneItems = {169405421, 169405422, 169405423};

	private static final int[] skills4WeaponShield = {
		13001, 13002, 13003, 13004, 13005, 13006, 13007, 13008, 13009,
		13010, 13011, 13012, 13013, 13014, 13015, 13016, 13017, 13018, 13019, 13020, 13021, 13022, 13023, 13024,
		13025, 13026, 13027, 13028, 13029, 13030, 13031, 13032, 13033, 13034, 13035, 13036, 13037, 13228, 13229,
		13230, 13231, 13232, 13233, 13234
	};

	private static final int[] skills4Glove = {
		13038, 13039, 13040, 13041, 13042, 13043, 13044, 13045, 13046, 13047,
		13048, 13049, 13050, 13051, 13052, 13053, 13054, 13055, 13056, 13057, 13058, 13059, 13060, 13247, 13248,
		13249, 13250, 13251, 13252, 13253, 13254
	};

	private static final int[] skills4Pant = {
		13061, 13062, 13063, 13064, 13065, 13066, 13067, 13068, 13069, 13070,
		13071, 13072, 13073, 13074, 13075, 13076, 13077, 13078, 13079, 13080, 13081
	};

	private static final int[] skills4Shoulder = {
		13082, 13083, 13084, 13085, 13086, 13087, 13088, 13089, 13090, 13091,
		13092, 13093, 13094, 13095, 13096, 13097, 13098, 13099, 13100, 13101, 13102, 13103, 13104, 13105, 13106,
		13107, 13266, 13267, 13268, 13269, 13270, 13271
	};

	private static final int[] skills4Shoes = {
		13108, 13109, 13110, 13111, 13112, 13113, 13114, 13115, 13116, 13117,
		13118, 13119, 13120, 13121, 13122, 13123, 13124, 13125, 13126, 13127
	};

	private static final int[] skills4Jacket = {
		13128, 13129, 13130, 13131, 13132, 13133, 13134, 13135, 13136, 13137,
		13138, 13139, 13140, 13141, 13142, 13143, 13144, 13145, 13146, 13147, 13235, 13236, 13237, 13238, 13239,
		13240, 13241, 13242, 13243, 13244, 13245, 13246
	};

	private static final int[] skills4Wing = {
		13001, 13002, 13003, 13004, 13005, 13006, 13007, 13008, 13009, 13010,
		13011, 13012, 13013, 13014, 13015, 13016, 13017, 13018, 13019, 13020, 13021, 13022, 13023, 13024, 13025,
		13026, 13027, 13028, 13029, 13030, 13031, 13032, 13033, 13034, 13035, 13036, 13037, 13228, 13229, 13230,
		13231, 13232, 13233, 13234
	};

	/**
	 * 获取装备最大强化等级（配置优先，默认 50，上限 200）。
	 * Returns max equipment enchant level (config first, default 50, capped at 200).
	 *
	 * @return 最大强化等级 / max enchant level
	 */
	public static int getMaxEquipmentEnchantLevel() {
		int configured = EnchantsConfig.MAX_EQUIPMENT_ENCHANT_LEVEL;
		if (configured <= 0) {
			configured = 50;
		}
		return Math.min(configured, 200);
	}

	/**
	 * 将强化等级限制在最大允许值内。
	 * Caps an enchant level to the maximum allowed value.
	 *
	 * @param enchantLevel 原始强化等级 / raw enchant level
	 * @return 限制后的等级 / capped level
	 */
	public static int capEquipmentEnchantLevel(int enchantLevel) {
		return Math.min(enchantLevel, getMaxEquipmentEnchantLevel());
	}

	/**
	 * 获取物品的觉醒等级上限（羽饰、手镯各取专用配置，其余饰品取首饰配置）。
	 * Returns the authorize cap for an item (plume and bracelet have their own keys, other accessories the accessory key).
	 *
	 * @param item 目标物品 / target item
	 * @return 觉醒等级上限；该物品不适用觉醒时返回 0 / authorize cap, or 0 when the item is not authorizable
	 */
	public static int getMaxAuthorizeLevel(Item item) {
		ItemTemplate template = item.getItemTemplate();
		if (!template.isAccessory() || template.getMaxAuthorize() == 0) {
			return 0;
		}
		int configured = template.isPlume() ? EnchantsConfig.MAX_AUTHORIZE_PLUME_LEVEL
			: template.isBracelet() ? EnchantsConfig.MAX_AUTHORIZE_BRACELET_LEVEL
			: EnchantsConfig.MAX_AUTHORIZE_ACCESSORY_LEVEL;
		return Math.min(configured, 200);
	}

	/**
	 * 将觉醒等级限制在配置上限内。
	 * Caps an authorize level to the configured maximum.
	 *
	 * @param item           目标物品 / target item
	 * @param authorizeLevel 原始觉醒等级 / raw authorize level
	 * @return 限制后的等级 / capped level
	 */
	public static int capAuthorizeLevel(Item item, int authorizeLevel) {
		int max = getMaxAuthorizeLevel(item);
		return max > 0 ? Math.min(authorizeLevel, max) : authorizeLevel;
	}

	/**
	 * 拆解物品为强化石粉末等材料。
	 * Breaks an item into materials such as enchantment stone dust.
	 *
	 * @param player     玩家 / player
	 * @param targetItem 目标物品 / target item
	 * @return 是否成功 / whether successful
	 */
	public static boolean breakItem(Player player, Item targetItem) {
		Storage inventory = player.getInventory();
		int kinah = breakKinah();
		int stone = 188100335; // 强化石粉末。 / Enchantment Stone Dust.
		if (inventory.getItemByObjId(targetItem.getObjectId()) == null) {
			return false;
		}
		ItemTemplate itemTemplate = targetItem.getItemTemplate();
		int quality = itemTemplate.getItemQuality().getQualityId();
		if (!itemTemplate.isArmor() && !itemTemplate.isWeapon()) {
			AuditLogger.info(player, "Player try break dont compatible item type.");
			return false;
		}
		if (player.getInventory().getKinah() < kinah) {
			return false;
		}
		player.getInventory().decreaseKinah(kinah);
		if (targetItem.getItemTemplate().isEstima() && targetItem.isEquipped()) {
			return false;
		}
		if (itemTemplate.isSoulBound() && !itemTemplate.isArmor()) {
			quality += 1;
		} else if (!itemTemplate.isSoulBound() && itemTemplate.isArmor()) {
			quality -= 1;
		}
		int number = switch (quality) {
			case 0, 1 -> Rnd.get(50, 80); // 垃圾 / JUNK, 普通 / COMMON
			case 2 -> Rnd.get(80, 160); // 稀有 / RARE
			case 3 -> Rnd.get(160, 320); // 传颂 / LEGEND
			case 4 -> Rnd.get(320, 640); // 唯一 / UNIQUE
			case 5 -> Rnd.get(640, 880); // 史诗 / EPIC
			case 6, 7 -> Rnd.get(880, 1000); // 神话 / MYTHIC
			default -> 0;
		};
		// 提取高阶守护者装备将获得强化石粉末与高阶守护者制作材料。 / Extracting Archdaeva equipment will give Enchantment Stone Dust and Archdaeva crafting materials.
		if (targetItem.isArchDaevaItem() && EnchantsConfig.ENABLE_ARCHDAEVA_ITEM_BROKE) {
			ItemService.addItem(player, RndArray.get(archDaevaStoneItems), 1);
		}
		if (inventory.delete(targetItem) != null) {
			ItemService.addItem(player, stone, number);
		} else {
			AuditLogger.info(player, "Possible break item hack, do not remove item.");
		}
		return true;
	}

	/**
	 * 拆解所需基纳。
	 * Kinah cost for breaking an item.
	 *
	 * @return 所需基纳 / kinah amount
	 */
	public static int breakKinah() {
		return 22600;
	}

	/**
	 * 执行 Estima 附魔流程。
	 * Runs the Estima enchant flow.
	 *
	 * @param player     玩家 / player
	 * @param parentItem 主物品 / parent item
	 * @param targetItem 目标物品 / target item
	 */
	public static void estimaEnchant(final Player player, final Item parentItem, final Item targetItem) {
		int estimaKinah = 34323;
		int chance = 100 - targetItem.getEnchantLevel();
		final boolean isEstimaSuccess = chance >= 60;
		if (player.getInventory().getKinah() < estimaKinah) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_MONEY);
			return;
		}
		player.getInventory().decreaseKinah(estimaKinah);
		final int parentItemId = parentItem.getItemId();
		final int parentObjectId = parentItem.getObjectId();
		final int parentNameId = parentItem.getNameId();
		final int targetEnchantLvl = targetItem.getEnchantLevel();
		PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), parentItem.getObjectId(), parentItemId, 5000, 0, 0), true);
		final ItemUseObserver observer = new ItemUseObserver() {
			@Override
			public void abort() {
				if (player.getController().cancelTask(TaskId.ITEM_USE) == null) {
					player.getObserveController().removeObserver(this);
					return;
				}
				player.removeItemCoolDown(parentItem.getItemTemplate().getUseLimits().getDelayId());
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_STIGMA_ENCHANT_CANCEL(new DescriptionId(parentNameId)));
				PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), parentObjectId, parentItemId, 0, 2, 0), true);
				player.getObserveController().removeObserver(this);
			}
		};
		player.getObserveController().attach(observer);
		player.getController().scheduleTask(TaskId.ITEM_USE, () -> {
			if (isEstimaSuccess) {
				player.getObserveController().removeObserver(observer);
				PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), parentObjectId, parentItemId, 0, 1, 1), true);
				player.getInventory().decreaseByObjectId(parentObjectId, 1);
				targetItem.setEnchantLevel(targetEnchantLvl + 1);
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_STIGMA_ENCHANT_SUCCESS(new DescriptionId(parentNameId)));
			} else {
				player.getInventory().decreaseByObjectId(parentObjectId, 1);
				targetItem.setEnchantLevel(0);
				PacketSendUtility.sendPacket(player, new SM_INVENTORY_UPDATE_ITEM(player, targetItem));
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_STIGMA_ENCHANT_FAIL(new DescriptionId(parentNameId)));
			}
			player.getObserveController().removeObserver(observer);
			PacketSendUtility.sendPacket(player, new SM_INVENTORY_UPDATE_ITEM(player, targetItem));
			ItemPacketService.updateItemAfterInfoChange(player, targetItem);
			PacketSendUtility.broadcastPacketAndReceive(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), parentItem.getObjectId(), parentItem.getItemTemplate().getTemplateId(), 0, isEstimaSuccess ? 1 : 2, 0));
		}, 5000);
	}

	/**
	 * 尝试对装备进行强化，计算成功率。
	 * Attempts to enchant equipment and computes success chance.
	 *
	 * @param player         玩家 / player
	 * @param parentItem     强化石 / enchant stone
	 * @param targetItem     目标物品 / target item
	 * @param supplementItem 辅助道具 / supplement
	 * @return 是否成功 / whether successful
	 */
	public static boolean enchantItem(Player player, Item parentItem, Item targetItem, Item supplementItem) {
		ItemTemplate enchantStone = parentItem.getItemTemplate();
		int enchantStoneLevel = enchantStone.getLevel();
		int targetItemLevel = targetItem.getItemTemplate().getLevel();
		int enchantItemLevel = targetItem.getEnchantLevel() + 1;
		int qualityCap = switch (targetItem.getItemTemplate().getItemQuality()) {
			case JUNK, COMMON, RARE -> 1;
			case LEGEND -> 2;
			case UNIQUE -> 3;
			case EPIC -> 4;
			case MYTHIC -> 5;
		};
		float success = EnchantsConfig.ENCHANT_ITEM;
		enchantStoneLevel = switch (parentItem.getItemId()) {
			case 166000196, 166000197 -> Rnd.get(105, 190);
			case 166010000, 166010001 -> Rnd.get(135, 220);
			case 166020000, 166020001, 166020002, 166020003, 166020004, 166020005, 166022003, 166022007 ->
				Rnd.get(165, 220);
			case 166022000 -> Rnd.get(200, 240);
			default -> enchantStoneLevel;
		};
		int levelDiff = enchantStoneLevel - targetItemLevel;
		success += levelDiff > 0 ? levelDiff * 3f / qualityCap : 0;
		success += levelDiff - qualityCap;
		success -= targetItem.getEnchantLevel() * qualityCap / (enchantItemLevel > 10 ? 5f : 6f);
		if (supplementItem != null) {
			int supplementUseCount = 1;
			ItemTemplate supplementTemplate = supplementItem.getItemTemplate();
			float addSuccessRate = 0f;
			EnchantItemAction action = supplementTemplate.getActions().getEnchantAction();
			if (action != null) {
				if (action.isManastoneOnly()) {
					return false;
				}
				addSuccessRate = action.getChance() * 2;
			}
			action = enchantStone.getActions().getEnchantAction();
			if (action != null) {
				supplementUseCount = action.getCount();
			}
			if (player.getInventory().getItemCountByItemId(supplementTemplate.getTemplateId()) < supplementUseCount) {
				return false;
			}
			switch (parentItem.getItemTemplate().getItemQuality()) {
				case UNIQUE, EPIC, MYTHIC -> {
					if (supplementUseCount > 149) {
						success = 100;
					} else if (supplementUseCount != 0) {
						success = 50;
					}
				}
				default -> {
				}
			}
			success += addSuccessRate;
			player.subtractSupplements(supplementUseCount, supplementTemplate.getTemplateId());
		}
		if (targetItem.isAmplified() && enchantStone.isAmplificationStone()) {
			success += 180 - targetItem.getEnchantLevel() * 1.0f;
		}
		if (player.isGM()) {
			success = 100;
		}
		boolean result = false;
		float random = Rnd.get(1, 1000) / 10f;
		if (random <= success) {
			result = true;
		}
		if (player.getAccessLevel() > 0) {
			PacketSendUtility.sendMessage(player, (result ? "Success" : "Fail") + " Rnd:" + random + " Luck:" + success);
		}
		return result;
	}

	/**
	 * 应用强化结果（成功加等级或失败处理）。
	 * Applies enchant result (level-up on success or failure handling).
	 *
	 * @param player         玩家 / player
	 * @param parentItem     强化石 / enchant stone
	 * @param targetItem     目标物品 / target item
	 * @param currentEnchant 当前强化等级 / current enchant
	 * @param result         是否成功 / success flag
	 */
	public static void enchantItemAct(Player player, Item parentItem, Item targetItem, int currentEnchant, boolean result) {
		int oldEnchant = currentEnchant;
		int addLevel = Rnd.get(1, 2);
		int critLevel = Rnd.get(1, 2);
		int EnchantKinah = EnchantService.EnchantKinah(targetItem);
		int rnd = Rnd.get(100);
		switch (parentItem.getItemId()) {
			case 166020000, 166020001, 166020002, 166020003, 166020004, 166020005, 166022003, 166022007 -> {
				if (rnd < 10) {
					addLevel = 3;
				} else if (rnd < 35) {
					addLevel = 2;
				}
			}
			case 166022000, 166022001, 166022002 -> {
				if (rnd < 7) {
					addLevel = 5;
				} else if (rnd < 25) {
					addLevel = 4;
				} else if (rnd < 50) {
					addLevel = 3;
				} else if (rnd <= 100) {
					addLevel = 2;
				}
			}
			default -> {
				if (rnd < 2) {
					addLevel = 3;
				} else if (rnd < 7) {
					addLevel = 2;
					critLevel = 2;
				}
			}
		}
		ItemQuality targetQuality = targetItem.getItemTemplate().getItemQuality();
		if (!player.getInventory().decreaseByObjectId(parentItem.getObjectId(), 1)) {
			AuditLogger.info(player, "Possible enchant hack, do not remove enchant stone.");
			return;
		}
		if (player.getInventory().getKinah() < EnchantKinah) {
			AuditLogger.info(player, "Possible enchant hack, Not depleted");
			return;
		}
		player.getInventory().decreaseKinah(EnchantKinah);
		player.updateSupplements();
		if (result || player.isGM()) {
			switch (targetQuality) {
				case UNIQUE, EPIC, MYTHIC -> {
					if (!EnchantsConfig.ENABLE_ARCHDAEVA_ITEM_BROKE && targetItem.isAmplified()
						&& parentItem.getItemId() >= 166020000 && parentItem.getItemId() <= 166020005
						&& parentItem.getItemId() >= 166000196 && parentItem.getItemId() <= 166000197) {
						currentEnchant += 1;
					} else if (targetItem.isAmplified() && parentItem.getItemId() >= 166022000 && parentItem.getItemId() <= 166022007) {
						currentEnchant += critLevel;
					} else if (currentEnchant + addLevel <= 15) {
						currentEnchant += addLevel;
					} else if (((addLevel - 1) > 1) && ((currentEnchant + addLevel - 1) <= 15)) {
						currentEnchant += (addLevel - 1);
					} else {
						currentEnchant += 1;
					}
				}
				default -> {
				}
			}
		} else {
			if (targetItem.isAmplified()) {
				int skillId = targetItem.getAmplificationSkill();
				int maxEnchant = targetItem.getItemTemplate().getMaxEnchantLevel();
				currentEnchant = maxEnchant;
				if (targetItem.getEnchantLevel() != maxEnchant) {
					targetItem.setAmplification(false);
				}
				targetItem.setAmplificationSkill(0);
				if (player.getSkillList().isSkillPresent(skillId)) {
					SkillLearnService.removeSkill(player, skillId);
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_EXCEED_SKILL_DELETE(new DescriptionId(targetItem.getNameId())));
				}
			} else if (currentEnchant >= 10 && currentEnchant <= 25) {
				currentEnchant = 10;
			} else if (currentEnchant > 0) {
				currentEnchant -= 1;
			}
		}
		currentEnchant = capEquipmentEnchantLevel(currentEnchant);
		targetItem.setEnchantLevel(currentEnchant);

		if (!targetItem.isAmplified() && targetItem.getEnchantLevel() == targetItem.getItemTemplate().getMaxEnchantLevel()) {
			targetItem.setAmplification(true);
		}
		if (targetItem.isEquipped()) {
			player.getGameStats().updateStatsVisually();
		}
		ItemPacketService.updateItemAfterInfoChange(player, targetItem, ItemUpdateType.STATS_CHANGE);
		if (targetItem.isEquipped()) {
			player.getEquipment().setPersistentState(PersistentState.UPDATE_REQUIRED);
		} else {
			player.getInventory().setPersistentState(PersistentState.UPDATE_REQUIRED);
		}
		// 强化置顶物品 4.7.5 / Strengthen Topped Item 4.7.5 http://aionpowerbook.com/powerbook/5.5_-_Enchanting_System
		if (targetItem.isAmplified() && targetItem.getEnchantLevel() == 20) {
			targetItem.setAmplificationSkill(getRndSkills(targetItem));
			targetItem.setPersistentState(PersistentState.UPDATE_REQUIRED);
			PacketSendUtility.sendPacket(player, new SM_INVENTORY_UPDATE_ITEM(player, targetItem));
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_EXCEED_SKILL_ENCHANT(new DescriptionId(targetItem.getNameId()), targetItem.getEnchantLevel(), getRndSkills(targetItem)));
			if (targetItem.isEquipped()) {
				player.getSkillList().addSkill(player, targetItem.getAmplificationSkill(), 1);
				player.getController().updatePassiveStats();
			}
		} else if (targetItem.getEnchantLevel() < 16) {
			targetItem.setAmplificationSkill(0);
			if (player.getSkillList().isSkillPresent(targetItem.getAmplificationSkill()) && targetItem.isEquipped()) {
				SkillLearnService.removeSkill(player, targetItem.getAmplificationSkill());
				targetItem.setPersistentState(PersistentState.UPDATE_REQUIRED);
				PacketSendUtility.sendPacket(player, new SM_INVENTORY_UPDATE_ITEM(player, targetItem));
			}
		}
		if (result) {
			int realIncrease = currentEnchant - oldEnchant;
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ENCHANT_ITEM_SUCCEED_NEW(new DescriptionId(targetItem.getNameId()), realIncrease));
			if (targetItem.isAmplified() && targetItem.getEnchantLevel() == targetItem.getItemTemplate().getMaxEnchantLevel()) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CAN_EXCEED_ENCHANT_LEVEL(new DescriptionId(targetItem.getNameId())));
			}
		} else {
			if (targetItem.isArchDaevaItem() && targetItem.isEquipped() && !EnchantItemRules.isArchdaevaReformedDanuar(targetItem)
				&& !EnchantItemRules.isArchdaevaRemodeledDanuar(targetItem) && !EnchantItemRules.isArchdaevaRestructuredDanuar(targetItem)
				&& !EnchantItemRules.isGrayWolfAccessories(targetItem) && !EnchantItemRules.isEnhancedAncientFallusha(targetItem)) {
				if (EnchantsConfig.ENABLE_ARCHDAEVA_ITEM_BROKE) {
					player.getEquipment().unEquipItem(targetItem.getObjectId());
					if (targetItem.hasGodStone()) {
						ItemService.addItem(player, targetItem.getGodStone().getItemId(), 1);
						for (ManaStone manaStone : targetItem.getItemStones()) {
							ItemService.addItem(player, manaStone.getItemId(), 1);
						}
					}
					ItemService.addItem(player, 188100335, 500); // 强化石粉末。 / Enchantment Stone Dust.
					ItemService.addItem(player, RndArray.get(archDaevaStoneItems), 1);
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ENCHANT_TYPE1_ENCHANT_FAIL(new DescriptionId(targetItem.getNameId())));
				}
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ENCHANT_ITEM_FAILED(new DescriptionId(targetItem.getNameId())));
				if (EnchantsConfig.ENABLE_ARCHDAEVA_ITEM_BROKE) {
					PacketSendUtility.sendPacket(player, new SM_INVENTORY_UPDATE_ITEM(player, targetItem));
					player.getGameStats().updateStatsVisually();
				}
			} else if (targetItem.isArchDaevaItem() && EnchantsConfig.ENABLE_ARCHDAEVA_ITEM_BROKE) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ENCHANT_TYPE1_ENCHANT_FAIL(new DescriptionId(targetItem.getNameId())));
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ENCHANT_ITEM_FAILED(new DescriptionId(targetItem.getNameId())));
			} else {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ENCHANT_ITEM_FAILED(new DescriptionId(targetItem.getNameId())));
			}
		}
	}

	/**
	 * 按物品类型随机获取技能 ID。
	 * Picks a random skill id for the item type.
	 *
	 * @param item 目标物品 / item
	 * @return 技能 ID / skill id
	 */
	public static int getRndSkills(Item item) {
		if (item.getItemTemplate().getArmorType() == ArmorType.WING) {
			return RndArray.get(skills4Wing);
		}
		return switch (item.getItemTemplate().getCategory()) {
			case SWORD, DAGGER, MACE, ORB, SPELLBOOK, GREATSWORD, POLEARM, STAFF, BOW, GUN, CANNON, HARP, KEYBLADE,
				 SHIELD -> RndArray.get(skills4WeaponShield);
			case JACKET -> RndArray.get(skills4Jacket);
			case PANTS -> RndArray.get(skills4Pant);
			case SHOULDERS -> RndArray.get(skills4Shoulder);
			case GLOVES -> RndArray.get(skills4Glove);
			case SHOES -> RndArray.get(skills4Shoes);
			default -> 0;
		};
	}

	/**
	 * 强化所需基纳。
	 * Kinah cost for enchanting.
	 *
	 * @param item 目标物品 / item
	 * @return 所需基纳 / kinah amount
	 */
	public static int EnchantKinah(Item item) {
		if (EnchantsConfig.ENCHANT_ITEM_KINAH >= 0) {
			return EnchantsConfig.ENCHANT_ITEM_KINAH;
		}
		if (item.getItemTemplate().getItemQuality() == ItemQuality.EPIC) {
			return switch (item.getEnchantLevel()) {
				case 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 -> 11441;
				case 15 -> 2825000;
				case 16 -> 4237500;
				case 17 -> 5650000;
				case 18 -> 7062500;
				case 19 -> 9887500;
				case 20 -> 12712500;
				case 21 -> 16950000;
				case 22 -> 22600000;
				case 23 -> 31075000;
				case 24 -> 42375000;
				default -> 57912500;
			};
		} else if (item.getItemTemplate().getItemQuality() == ItemQuality.MYTHIC) {
			return switch (item.getEnchantLevel()) {
				case 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 -> 34323;
				case 15 -> 6356250;
				case 16 -> 8475000;
				case 17 -> 11300000;
				case 18 -> 15537500;
				case 19 -> 21187500;
				case 20 -> 28250000;
				case 21 -> 38137500;
				case 22 -> 50850000;
				case 23 -> 69212500;
				case 24 -> 93225000;
				default -> 125712500;
			};
		} else {
			return 0;
		}
	}

	/**
	 * 尝试镶嵌魔力石并计算成功率。
	 * Attempts manastone socketing and computes success chance.
	 *
	 * @param player         玩家 / player
	 * @param parentItem     魔力石 / manastone
	 * @param targetItem     目标物品 / target item
	 * @param supplementItem 辅助道具 / supplement
	 * @param targetWeapon   目标武器槽位标识 / target weapon flag
	 * @return 是否成功 / whether successful
	 */
	public static boolean socketManastone(Player player, Item parentItem, Item targetItem, Item supplementItem, int targetWeapon) {
		int targetItemLevel = (targetWeapon == 1)
			? targetItem.getItemTemplate().getLevel()
			: targetItem.getFusionedItemTemplate().getLevel();
		int stoneLevel = parentItem.getItemTemplate().getLevel();
		int slotLevel = (int) (10 * Math.ceil((targetItemLevel + 10) / 10d));
		if (stoneLevel > slotLevel) {
			return false;
		}
		int stoneCount = (targetWeapon == 1)
			? targetItem.getItemStones().size()
			: targetItem.getFusionStones().size();
		if (targetWeapon == 1) {
			if (stoneCount >= targetItem.getSockets(false)) {
				AuditLogger.info(player, "Manastone socket overload");
				return false;
			}
		} else if (!targetItem.hasFusionedItem() || stoneCount >= targetItem.getSockets(true)) {
			AuditLogger.info(player, "Manastone socket overload");
			return false;
		}
		float success = EnchantsConfig.SOCKET_MANASTONE;
		success += parentItem.getItemTemplate().getItemQuality() == ItemQuality.COMMON ? 25f : 15f;
		float socketDiff = stoneCount * 1.25f + 1.75f;
		success += (slotLevel - stoneLevel) / socketDiff;
		if (supplementItem != null) {
			int supplementUseCount = 0;
			ItemTemplate manastoneTemplate = parentItem.getItemTemplate();
			int manastoneCount = (targetWeapon == 1 ? targetItem.getItemStones().size() : targetItem.getFusionStones().size()) + 1;
			ItemTemplate supplementTemplate = supplementItem.getItemTemplate();
			float addSuccessRate = 0f;
			boolean isManastoneOnly = false;
			EnchantItemAction action = manastoneTemplate.getActions().getEnchantAction();
			if (action != null) {
				supplementUseCount = action.getCount();
			}
			action = supplementTemplate.getActions().getEnchantAction();
			if (action != null) {
				addSuccessRate = action.getChance();
				isManastoneOnly = action.isManastoneOnly();
			}
			switch (parentItem.getItemTemplate().getItemQuality()) {
				case RARE -> addSuccessRate *= 80;
				case LEGEND, UNIQUE -> addSuccessRate *= 70;
				case EPIC, MYTHIC -> addSuccessRate *= 60;
				default -> {
				}
			}
			if (isManastoneOnly) {
				supplementUseCount = 1;
			} else if (stoneCount > 0) {
				supplementUseCount = supplementUseCount * manastoneCount;
			}
			if (player.getInventory().getItemCountByItemId(supplementTemplate.getTemplateId()) < supplementUseCount) {
				return false;
			}
			success += addSuccessRate;
			player.subtractSupplements(supplementUseCount, supplementTemplate.getTemplateId());
		}
		float random = Rnd.get(1, 1000) / 10f;
		return random <= success;
	}

	/**
	 * 应用魔力石镶嵌结果。
	 * Applies the manastone socketing result.
	 *
	 * @param player       玩家 / player
	 * @param parentItem   魔力石 / manastone
	 * @param targetItem   目标物品 / target item
	 * @param targetWeapon 目标武器槽位标识 / target weapon flag
	 * @param result       是否成功 / success flag
	 */
	public static void socketManastoneAct(Player player, Item parentItem, Item targetItem, int targetWeapon, boolean result) {
		player.updateSupplements();
		int manastoneKinah = 17161;
		if (player.getInventory().decreaseByObjectId(parentItem.getObjectId(), 1) && result) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_GIVE_ITEM_OPTION_SUCCEED(new DescriptionId(targetItem.getNameId())));
			if (player.getInventory().getKinah() < manastoneKinah) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_MONEY);
				return;
			}
			player.getInventory().decreaseKinah(manastoneKinah);
			ManaStone manaStone = (targetWeapon == 1)
				? ItemSocketService.addManaStone(targetItem, parentItem.getItemTemplate().getTemplateId())
				: ItemSocketService.addFusionStone(targetItem, parentItem.getItemTemplate().getTemplateId());
			if (targetItem.isEquipped()) {
				ItemEquipmentListener.addStoneStats(manaStone, player.getGameStats());
				player.getGameStats().updateStatsAndSpeedVisually();
			}
		} else {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_GIVE_ITEM_OPTION_FAILED(new DescriptionId(targetItem.getNameId())));
		}
		ItemPacketService.updateItemAfterInfoChange(player, targetItem);
	}

	/**
	 * 装备穿戴时应用强化相关属性修正。
	 * Applies enchant-related stat modifiers when an item is equipped.
	 *
	 * @param player 玩家 / player
	 * @param item   物品 / item
	 */
	public static void onItemEquip(Player player, Item item) {
		List<IStatFunction> modifiers = new ArrayList<>();
		try {
			if (item.getItemTemplate().isWeapon()) {
				switch (item.getItemTemplate().getWeaponType()) {
					case ORB_2H, BOOK_2H, GUN_1H, HARP_2H, CANNON_2H, KEYBLADE_2H -> {
						modifiers.add(new StatEnchantFunction(item, StatEnum.BOOST_MAGICAL_SKILL, 0));
						modifiers.add(new StatEnchantFunction(item, StatEnum.MAGICAL_ATTACK, 0));
					}
					case MACE_1H, STAFF_2H -> {
						modifiers.add(new StatEnchantFunction(item, StatEnum.MAGICAL_ATTACK, 0));
						modifiers.add(new StatEnchantFunction(item, StatEnum.PHYSICAL_ATTACK, 0));
						modifiers.add(new StatEnchantFunction(item, StatEnum.BOOST_MAGICAL_SKILL, 0));
					}
					case SWORD_1H, DAGGER_1H -> {
						if (item.getEquipmentSlot() == ItemSlot.MAIN_HAND.getSlotIdMask()) {
							modifiers.add(new StatEnchantFunction(item, StatEnum.MAIN_HAND_POWER, 0));
						} else {
							modifiers.add(new StatEnchantFunction(item, StatEnum.OFF_HAND_POWER, 0));
						}
					}
					case SWORD_2H, BOW, POLEARM_2H ->
						modifiers.add(new StatEnchantFunction(item, StatEnum.PHYSICAL_ATTACK, 0));
					default -> {
					}
				}
			} else if (item.getItemTemplate().isArmor()) {
				if (item.getItemTemplate().getArmorType() == ArmorType.SHIELD) {
					modifiers.add(new StatEnchantFunction(item, StatEnum.DAMAGE_REDUCE, 0));
					modifiers.add(new StatEnchantFunction(item, StatEnum.BLOCK, 0));
					modifiers.add(new StatEnchantFunction(item, StatEnum.MAXHP, 0));
					modifiers.add(new StatEnchantFunction(item, StatEnum.PHYSICAL_DEFENSE, 0));
					modifiers.add(new StatEnchantFunction(item, StatEnum.MAGIC_SKILL_BOOST_RESIST, 0));
				} else if (item.getItemTemplate().getItemSlot() == 32768) {
					modifiers.add(new StatEnchantFunction(item, StatEnum.PHYSICAL_ATTACK, 0));
					modifiers.add(new StatEnchantFunction(item, StatEnum.BOOST_MAGICAL_SKILL, 0));
					modifiers.add(new StatEnchantFunction(item, StatEnum.MAXHP, 0));
					modifiers.add(new StatEnchantFunction(item, StatEnum.PHYSICAL_CRITICAL_RESIST, 0));
					modifiers.add(new StatEnchantFunction(item, StatEnum.FLY_TIME, 0));
					modifiers.add(new StatEnchantFunction(item, StatEnum.MAGICAL_CRITICAL_RESIST, 0));
					modifiers.add(new StatEnchantFunction(item, StatEnum.SOAR_SPEED, 0));
				} else if (item.getItemTemplate().getCategory() == ItemCategory.PLUME) {
					int plumeId = item.getItemTemplate().getTemperingTableId();
					switch (plumeId) {
						case 10051, 10063 -> {
							modifiers.add(new StatEnchantFunction(item, StatEnum.PHYSICAL_ATTACK, 0));
							modifiers.add(new StatEnchantFunction(item, StatEnum.MAXHP, 0));
						}
						case 10052, 10064 -> {
							modifiers.add(new StatEnchantFunction(item, StatEnum.BOOST_MAGICAL_SKILL, 0));
							modifiers.add(new StatEnchantFunction(item, StatEnum.MAXHP, 0));
						}
						case 10056, 10065 -> {
							modifiers.add(new StatEnchantFunction(item, StatEnum.PHYSICAL_CRITICAL, 0));
							modifiers.add(new StatEnchantFunction(item, StatEnum.MAXHP, 0));
						}
						case 10057, 10066 -> {
							modifiers.add(new StatEnchantFunction(item, StatEnum.MAGICAL_ACCURACY, 0));
							modifiers.add(new StatEnchantFunction(item, StatEnum.MAXHP, 0));
						}
						case 10103 -> {
							modifiers.add(new StatEnchantFunction(item, StatEnum.MAXHP, 0));
							modifiers.add(new StatEnchantFunction(item, StatEnum.PHYSICAL_ACCURACY, 0));
						}
						case 10104 -> {
							modifiers.add(new StatEnchantFunction(item, StatEnum.MAXHP, 0));
							modifiers.add(new StatEnchantFunction(item, StatEnum.MAGICAL_CRITICAL, 0));
						}
						case 10105 -> {
							modifiers.add(new StatEnchantFunction(item, StatEnum.PHYSICAL_ATTACK, 0));
							modifiers.add(new StatEnchantFunction(item, StatEnum.MAXHP, 0));
							modifiers.add(new StatEnchantFunction(item, StatEnum.PHYSICAL_ACCURACY, 0));
						}
						case 10106 -> {
							modifiers.add(new StatEnchantFunction(item, StatEnum.BOOST_MAGICAL_SKILL, 0));
							modifiers.add(new StatEnchantFunction(item, StatEnum.MAXHP, 0));
							modifiers.add(new StatEnchantFunction(item, StatEnum.MAGICAL_CRITICAL, 0));
						}
						case 10107 -> {
							modifiers.add(new StatEnchantFunction(item, StatEnum.PHYSICAL_CRITICAL, 0));
							modifiers.add(new StatEnchantFunction(item, StatEnum.MAXHP, 0));
							modifiers.add(new StatEnchantFunction(item, StatEnum.PHYSICAL_ATTACK, 0));
						}
						case 10108 -> {
							modifiers.add(new StatEnchantFunction(item, StatEnum.MAGICAL_ACCURACY, 0));
							modifiers.add(new StatEnchantFunction(item, StatEnum.MAXHP, 0));
							modifiers.add(new StatEnchantFunction(item, StatEnum.BOOST_MAGICAL_SKILL, 0));
						}
						case 10109 -> {
							modifiers.add(new StatEnchantFunction(item, StatEnum.PHYSICAL_ATTACK, 0));
							modifiers.add(new StatEnchantFunction(item, StatEnum.MAXHP, 0));
							modifiers.add(new StatEnchantFunction(item, StatEnum.PHYSICAL_CRITICAL, 0));
						}
						case 10110 -> {
							modifiers.add(new StatEnchantFunction(item, StatEnum.BOOST_MAGICAL_SKILL, 0));
							modifiers.add(new StatEnchantFunction(item, StatEnum.MAXHP, 0));
							modifiers.add(new StatEnchantFunction(item, StatEnum.MAGICAL_ACCURACY, 0));
						}
						default -> {
						}
					}
				} else {
					modifiers.add(new StatEnchantFunction(item, StatEnum.PHYSICAL_ATTACK, 0));
					modifiers.add(new StatEnchantFunction(item, StatEnum.BOOST_MAGICAL_SKILL, 0));
					modifiers.add(new StatEnchantFunction(item, StatEnum.PHYSICAL_DEFENSE, 0));
					modifiers.add(new StatEnchantFunction(item, StatEnum.MAGICAL_DEFEND, 0));
					modifiers.add(new StatEnchantFunction(item, StatEnum.MAXHP, 0));
					modifiers.add(new StatEnchantFunction(item, StatEnum.PHYSICAL_CRITICAL_RESIST, 0));
				}
			} else if (item.getItemTemplate().isAccessory()) {
				if (item.getItemTemplate().getTemperingTableId() > 0) {
					ItemEnchantTemplate ie = DataManager.ITEM_ENCHANT_DATA.getEnchantTemplate(EnchantType.AUTHORIZE, item.getItemTemplate().getTemperingTableId());
					if (item.getAuthorize() > 0) {
						List<StatFunction> stats = ie == null ? null : ie.getStats(item.getAuthorize());
						if (stats != null) {
							modifiers.addAll(stats);
						} else {
							log.error(I18n.get("log.ee17dabb4951", item.getItemId(), item.getAuthorize()));
						}
					}
				} else {
					switch (item.getItemTemplate().getCategory()) {
						case HELMET, EARRINGS, NECKLACE ->
							modifiers.add(new StatEnchantFunction(item, StatEnum.PVP_ATTACK_RATIO, 0));
						case RINGS, BELT -> modifiers.add(new StatEnchantFunction(item, StatEnum.PVP_DEFEND_RATIO, 0));
						case BRACELET -> {
							modifiers.add(new StatEnchantFunction(item, StatEnum.PVP_ATTACK_RATIO, 0));
							modifiers.add(new StatEnchantFunction(item, StatEnum.PVP_DEFEND_RATIO, 0));
						}
						default -> {
						}
					}
				}
			}
			if (!modifiers.isEmpty()) {
				player.getGameStats().addEffect(item, modifiers);
			}
		} catch (Exception ex) {
			log.error(I18n.get("log.d3559bc2bf1a"), ex);
		}
	}

	/**
	 * 计算强化相关等级系数。
	 * Computes an enchant-related level factor.
	 *
	 * @param item 目标物品 / item
	 * @return 等级系数 / level factor
	 */
	public static int EnchantLevel(Item item) {
		if (item.getItemTemplate().isWeapon() || item.getItemTemplate().getArmorType() == ArmorType.SHIELD) {
			if (item.getEnchantLevel() >= item.getItemTemplate().getMaxEnchantLevel() && item.getEnchantLevel() < 16 || item.getItemTemplate().getMaxEnchantLevel() == 0) {
				return 1;
			} else if (item.getEnchantLevel() >= 16) {
				return 4;
			} else {
				return 0;
			}
		} else if (item.getItemTemplate().getArmorType() == ArmorType.PLUME) {
			if (item.getAuthorize() >= 5 && item.getAuthorize() < 10) {
				return 8;
			} else if (item.getAuthorize() >= 10) {
				return 16;
			} else {
				return 0;
			}
		} else if (item.getItemTemplate().getArmorType() == ArmorType.BRACELET) {
			if (item.getAuthorize() >= 5 && item.getAuthorize() < 10) {
				return 96;
			} else if (item.getAuthorize() >= 10) {
				return 160;
			} else {
				return 32;
			}
		}
		return 0;
	}

	public static void GloryShieldSkill(Player player) {
		EnchantItemRules.GloryShieldSkill(player);
	}

	/**
	 * 应用物品推荐等级削减结果。
	 * Applies recommended-level reduction result on an item.
	 *
	 * @param player           玩家 / player
	 * @param parentItem       主物品 / parent item
	 * @param targetItem       目标物品 / target item
	 * @param currentReduction 当前削减值 / current reduction
	 * @param result           是否成功 / success flag
	 * @param count            数量 / count
	 */
	public static void reductItemAct(Player player, Item parentItem, Item targetItem, int currentReduction, boolean result, int count) {
		if (!result) {
			PacketSendUtility.broadcastPacketAndReceive(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), player.getObjectId(), parentItem.getObjectId(), parentItem.getItemId(), 0, 2, 0));
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_EQUIPLEVEL_ADJ_FAIL(targetItem.getNameId()));
		} else {
			PacketSendUtility.broadcastPacketAndReceive(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), player.getObjectId(), parentItem.getObjectId(), parentItem.getItemId(), 0, 1, 0));
			if (currentReduction + count > 5) {
				targetItem.setReductionLevel(5);
			} else {
				targetItem.setReductionLevel(currentReduction + count);
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_EQUIPLEVEL_ADJ_SUCCEED(targetItem.getNameId(), count));
			}
			if (targetItem.getReductionLevel() == 5) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_EQUIPLEVEL_ADJ_SUCCEED_MAX(targetItem.getNameId()));
			}
		}
		PacketSendUtility.sendPacket(player, new SM_INVENTORY_UPDATE_ITEM(player, targetItem));
		if (targetItem.isEquipped()) {
			player.getGameStats().updateStatsVisually();
		}
		ItemPacketService.updateItemAfterInfoChange(player, targetItem);
		if (targetItem.isEquipped()) {
			player.getEquipment().setPersistentState(PersistentState.UPDATE_REQUIRED);
		} else {
			player.getInventory().setPersistentState(PersistentState.UPDATE_REQUIRED);
		}
	}
}
