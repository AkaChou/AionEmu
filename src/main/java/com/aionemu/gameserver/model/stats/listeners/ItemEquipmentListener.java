package com.aionemu.gameserver.model.stats.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.configs.main.ArchDaevaConfig;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.IdianStone;
import com.aionemu.gameserver.model.items.ItemSlot;
import com.aionemu.gameserver.model.items.ManaStone;
import com.aionemu.gameserver.model.items.RandomStats;
import com.aionemu.gameserver.model.stats.calc.functions.StatAddFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatFunction;
import com.aionemu.gameserver.model.stats.container.CreatureGameStats;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.item.WeaponStats;
import com.aionemu.gameserver.model.templates.item.WeaponType;
import com.aionemu.gameserver.model.templates.itemset.FullBonus;
import com.aionemu.gameserver.model.templates.itemset.ItemSetTemplate;
import com.aionemu.gameserver.model.templates.itemset.PartBonus;
import com.aionemu.gameserver.services.EnchantService;
import com.aionemu.gameserver.services.SkillLearnService;

/**
 * 物品装备监听器，用于属性相关逻辑。
 * Item Equipment Listener for stats logic.
 *
 * @author xavier modified by Wakizashi
 */
@Slf4j
public class ItemEquipmentListener {

	/**
	 * @param item
	 * @param owner
	 */
	public static void onItemEquipment(Item item, Player owner) {
		owner.getController().cancelUseItem();
		ItemTemplate itemTemplate = item.getItemTemplate();

		onItemEquipment(item, owner.getGameStats(), owner);

		// 检查是否属于物品套装 / Check if belongs to ItemSet
		if (itemTemplate.isItemSet()) {
			recalculateItemSet(itemTemplate.getItemSet(), owner, item.getItemTemplate().isWeapon());
		}
		if (item.hasManaStones()) {
			addStonesStats(item, item.getItemStones(), owner.getGameStats());
		}
		if (item.hasFusionStones()) {
			addStonesStats(item, item.getFusionStones(), owner.getGameStats());
		}
		IdianStone idianStone = item.getIdianStone();
		if (idianStone != null) {
			idianStone.onEquip(owner);
		}
		addGodstoneEffect(owner, item);
		RandomStats randomStats = item.getRandomStats();
		if (randomStats != null) {
			randomStats.onEquip(owner);
		}
		if (item.getConditioningInfo() != null) {
			owner.getObserveController().addObserver(item.getConditioningInfo());
			item.getConditioningInfo().setPlayer(owner);
		}
		if (item.getAmplificationSkill() > 0) {
			owner.getSkillList().addSkill(owner, item.getAmplificationSkill(), 1);
			owner.getController().updatePassiveStats();
		}
		if (item.getItemSkinSkill() > 0) {
			owner.getSkillList().addSkill(owner, item.getItemSkinSkill(), 1);
		}
		EnchantService.GloryShieldSkill(owner);
		EnchantService.onItemEquip(owner, item);
	}

	/**
	 * @param item
	 * @param owner
	 */
	public static void onItemUnequipment(Item item, Player owner) {
		owner.getController().cancelUseItem();

		ItemTemplate itemTemplate = item.getItemTemplate();
		// 检查是否属于物品套装 / Check if belongs to ItemSet
		if (itemTemplate.isItemSet()) {
			recalculateItemSet(itemTemplate.getItemSet(), owner, item.getItemTemplate().isWeapon());
		}
		owner.getGameStats().endEffect(item);

		if (item.hasManaStones()) {
			removeStoneStats(item.getItemStones(), owner.getGameStats());
		}

		if (item.hasFusionStones()) {
			removeStoneStats(item.getFusionStones(), owner.getGameStats());
		}
		if (item.getConditioningInfo() != null) {
			owner.getObserveController().removeObserver(item.getConditioningInfo());
			item.getConditioningInfo().setPlayer(null);
		}
		IdianStone idianStone = item.getIdianStone();
		if (idianStone != null) {
			idianStone.onUnEquip(owner);
		}
		removeGodstoneEffect(owner, item);
		RandomStats randomStats = item.getRandomStats();
		if (randomStats != null) {
			randomStats.onUnEquip(owner);
		}
		
		/**
	 * 卸装时强化技能须与效果一并移除，仅保留长期技能效果。 / onItemUnequipment Amplify skill must be removed same as effect. We leave only effects from long time playing skills. after that we must to update passive skills stats
	 */
		if (item.getAmplificationSkill() > 0) {
			if (owner.getSkillList().isSkillPresent(item.getAmplificationSkill())) {
				if (item.getAmplificationSkill() == 13030 || item.getAmplificationSkill() == 13029) {
					// 此刻此处什么也不做 / dont do nothing here at this moment
				} else {
					owner.getEffectController().removeEffect(item.getAmplificationSkill());
				}
				SkillLearnService.removeSkill(owner, item.getAmplificationSkill());

				owner.getGameStats().endEffect(item);
			}
		}

		if (item.getItemSkinSkill() > 0) {
			if (owner.getSkillList().isSkillPresent(item.getItemSkinSkill())) {
				SkillLearnService.removeSkill(owner, item.getItemSkinSkill());
			}
		}
		EnchantService.GloryShieldSkill(owner);
	}

	/**
	 * @param item
	 * @param cgs
	 * @param player
	 */
	private static void onItemEquipment(Item item, CreatureGameStats<?> cgs, Player player) {
		ItemTemplate itemTemplate = item.getItemTemplate();
		long slot = item.getEquipmentSlot();
		List<StatFunction> modifiers = itemTemplate.getModifiers();
		if (modifiers == null) {
			return;
		}

		List<StatFunction> allModifiers = null;
		if ((slot & ItemSlot.MAIN_OR_SUB.getSlotIdMask()) != 0) {
			allModifiers = wrapModifiers(item, modifiers);
			if (item.hasFusionedItem()) {
				// 按规则添加全部加成修正 / add all bonus modifiers according to rules
				ItemTemplate fusionedItemTemplate = item.getFusionedItemTemplate();
				WeaponType weaponType = fusionedItemTemplate.getWeaponType();
				List<StatFunction> fusionedItemModifiers = fusionedItemTemplate.getModifiers();
				if (fusionedItemModifiers != null) {
					allModifiers.addAll(wrapModifiers(item, fusionedItemModifiers));
				}
				// 添加 10% 魔法增强与攻击 / add 10% of Magic Boost and Attack
				WeaponStats weaponStats = fusionedItemTemplate.getWeaponStats();
				if (weaponStats != null) {
					int boostMagicalSkill = Math.round(0.1f * weaponStats.getBoostMagicalSkill());
					int attack = Math.round(0.1f * weaponStats.getMeanDamage());
					if (weaponType == WeaponType.ORB_2H || weaponType == WeaponType.BOOK_2H
							|| weaponType == WeaponType.GUN_1H || // 4.3
							weaponType == WeaponType.CANNON_2H || // 4.3
							weaponType == WeaponType.HARP_2H || // 4.3
							weaponType == WeaponType.KEYBLADE_2H) { // 4.5
						allModifiers.add(new StatAddFunction(StatEnum.MAGICAL_ATTACK, attack, false));
						allModifiers.add(new StatAddFunction(StatEnum.BOOST_MAGICAL_SKILL, boostMagicalSkill, false));
					} else {
						allModifiers.add(new StatAddFunction(StatEnum.MAIN_HAND_POWER, attack, false));
					}
				}
			}
			if (ArchDaevaConfig.ITEM_NOT_FOR_HIGHDAEVA_ENABLE) {
				if (player.getLevel() >= 65 && !itemTemplate.isArchdaeva()) {
					for (StatFunction a : modifiers) {
						int value = a.getValue();
						int formula = (int) (value * (20.0f / 100.0f));
						allModifiers.add(new StatAddFunction(a.getName(), -formula, false));
					}
				}
			}
			// 高阶守护者物品等级限制 / ArchDaeva item level limitations
			if (player.getLevel() >= 65 && itemTemplate.isArchdaeva()) {
				int pLevel = player.getLevel();
				int iLevel = itemTemplate.getLevel();
				float percentageDecrease = 0;
				if (iLevel - pLevel == 1) {
					percentageDecrease = 2.0f;
				} else if (iLevel - pLevel == 2) {
					percentageDecrease = 4.0f;
				} else if (iLevel - pLevel == 3) {
					percentageDecrease = 6.0f;
				} else if (iLevel - pLevel == 4) {
					percentageDecrease = 8.0f;
				} else if (iLevel - pLevel == 5) {
					percentageDecrease = 10.0f;
				} else if (iLevel - pLevel == 6) {
					percentageDecrease = 12.0f;
				} else if (iLevel - pLevel == 7) {
					percentageDecrease = 14.0f;
				} else if (iLevel - pLevel == 8) {
					percentageDecrease = 16.0f;
				} else if (iLevel - pLevel == 9) {
					percentageDecrease = 18.0f;
				} else if (iLevel - pLevel == 10) {
					percentageDecrease = 20.0f;
				}
				for (StatFunction a : modifiers) {
					int value = a.getValue();
					int formula = (int) (value * (percentageDecrease / 100.0f));
					allModifiers.add(new StatAddFunction(a.getName(), -formula, false));
				}
			}
		} else {
			allModifiers = modifiers;
		}
		item.setCurrentModifiers(allModifiers);
		cgs.addEffect(item, allModifiers);
	}

	/**
	 * 按规则过滤属性：融合属性仅取自一把武器等。 / Filter stats based on the following rules:<br> 1) don't include fusioned stats which will be taken only from 1 weapon <br> 2) wrap stats which are different for MAIN and OFF hands<br> 3) add the rest<br>.
	 */
	private static List<StatFunction> wrapModifiers(Item item, List<StatFunction> modifiers) {
		List<StatFunction> allModifiers = new ArrayList<StatFunction>();
		for (StatFunction modifier : modifiers) {
			switch (modifier.getName()) {
			// 为何被移除见 DuplicateStatFunction / why they are removed look at DuplicateStatFunction
			case ATTACK_SPEED:
			case PVP_ATTACK_RATIO:
			case PVP_DEFEND_RATIO:
			case BOOST_CASTING_TIME:
				continue;
			default:
				allModifiers.add(modifier);
			}
		}
		return allModifiers;
	}

	/**
	 * @param itemSetTemplate
	 * @param player
	 * @param isWeapon
	 */
	private static void recalculateItemSet(ItemSetTemplate itemSetTemplate, Player player, boolean isWeapon) {
		if (itemSetTemplate == null) {
			return;
		}
		player.getGameStats().endEffect(itemSetTemplate);
		// 1.- 检查装备中是否已有该 itemSetTemplate id 的物品。 / 1.- Check equipment for items already equip with this itemSetTemplate id
		int itemSetPartsEquipped = player.getEquipment().itemSetPartsEquipped(itemSetTemplate.getId());

		// 若主手与副手相同，无加成 / If main hand and off hand is same , no bonus
		int mainHandItemId = 0;
		int offHandItemId = 0;
		if (player.getEquipment().getMainHandWeapon() != null) {
			mainHandItemId = player.getEquipment().getMainHandWeapon().getItemId();
		}
		if (player.getEquipment().getOffHandWeapon() != null) {
			offHandItemId = player.getEquipment().getOffHandWeapon().getItemId();
		}
		boolean mainAndOffNotSame = mainHandItemId != offHandItemId;

		// 2.- 检查物品套装部件，若未应用则逐个添加效果。 / 2.- Check Item Set Parts and add effects one by one if not done already
		for (PartBonus itempartbonus : itemSetTemplate.getPartbonus()) {
			if (mainAndOffNotSame && isWeapon) {
				// 若部件加成此前未应用，现在应用。 / If the partbonus was not applied before, do it now
				if (itempartbonus.getCount() <= itemSetPartsEquipped) {
					if (itempartbonus.getModifiers() != null) {
						player.getGameStats().addEffect(itemSetTemplate, itempartbonus.getModifiers());
					}
				}
			} else if (!isWeapon) {
				// 若部件加成此前未应用，现在应用。 / If the partbonus was not applied before, do it now
				if (itempartbonus.getCount() <= itemSetPartsEquipped) {
					player.getGameStats().addEffect(itemSetTemplate, itempartbonus.getModifiers());
				}
			}
		}

		// 3.- 最后检查是否已应用全部物品；否则设置完整加成。 / 3.- Finally check if all items are applied and set the full bonus if not
		// 已应用 / already applied
		FullBonus fullbonus = itemSetTemplate.getFullbonus();
		if (fullbonus != null && itemSetPartsEquipped == fullbonus.getCount()) {
			// 用 index=总部件数+1 添加完整加成，避免与部件混淆。 / Add the full bonus with index = total parts + 1 to avoid confusion with part
			// 加成等于数量 / bonus equal to number of
			// 对象 / objects
			player.getGameStats().addEffect(itemSetTemplate, fullbonus.getModifiers());
		}
	}

	/**
	 * 将所有魔石修正应用于角色。 / All modifiers of stones will be applied to character.
	 */
	private static void addStonesStats(Item item, Set<? extends ManaStone> itemStones, CreatureGameStats<?> cgs) {
		if (itemStones == null || itemStones.size() == 0) {
			return;
		}
        for (ManaStone stone : itemStones) {
            if (stone != null) { // Добавляем проверку
               addStoneStats(item, stone, cgs);
            }
        }
	}

	/**
	 * 对已装备物品镶嵌时使用。 / Used when socketing of equipped item.
	 */
	public static void addStoneStats(Item item, ManaStone stone, CreatureGameStats<?> cgs) {
        if (stone == null) {
           return;
        }
		List<StatFunction> modifiers = stone.getModifiers();
		if (modifiers == null) {
			return;
		}
		cgs.addEffect(stone, modifiers);
	}

	/**
	 * 移除所有魔石修正。 / All modifiers of stones will be removed.
	 */
	public static void removeStoneStats(Set<? extends ManaStone> itemStones, CreatureGameStats<?> cgs) {
		if (itemStones == null || itemStones.size() == 0) {
			return;
		}
		for (ManaStone stone : itemStones) {
			List<StatFunction> modifiers = stone.getModifiers();
			if (modifiers != null) {
				cgs.endEffect(stone);
			}
		}
	}

	/** 移除 stone stats 1 / Removes stone stats 1 */
	public static void removeStoneStats1(Item item, ManaStone stone, CreatureGameStats<?> cgs) {
		if (stone == null || item == null) {
			return;
		}
		List<StatFunction> modifiers = stone.getModifiers();
		if (modifiers == null) {
			return;
		}

		cgs.endEffect(stone);
	}

	/**
	 * @param player
	 */
	private static void addGodstoneEffect(Player player, Item item) {
		if (item.getGodStone() != null) {
			item.getGodStone().onEquip(player);
		}
	}

	/**
	 * @param player
	 */
	private static void removeGodstoneEffect(Player player, Item item) {
		if (item.getGodStone() != null) {
			item.getGodStone().onUnEquip(player);
		}
	}

	/** 添加伊迪安加成属性。 / Adds idian bonus stats. */
	public static void addIdianBonusStats(Item item, List<StatFunction> modifiers, CreatureGameStats<?> cgs) {
		cgs.addEffect(item, modifiers);
	}

	/** 移除伊迪安加成属性。 / Removes idian bonus stats. */
	public static void removeIdianBonusStats(Item item, CreatureGameStats<?> cgs) {
		cgs.endEffect(item);
	}
}
