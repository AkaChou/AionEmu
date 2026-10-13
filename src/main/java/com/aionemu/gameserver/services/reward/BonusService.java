package com.aionemu.gameserver.services.reward;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.ItemGroupsData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.itemgroups.BonusItemGroup;
import com.aionemu.gameserver.model.templates.itemgroups.CraftGroup;
import com.aionemu.gameserver.model.templates.itemgroups.ItemRaceEntry;
import com.aionemu.gameserver.model.templates.itemgroups.ManastoneGroup;
import com.aionemu.gameserver.model.templates.itemgroups.MedalGroup;
import com.aionemu.gameserver.model.templates.quest.QuestBonuses;
import com.aionemu.gameserver.model.templates.quest.QuestItems;
import com.aionemu.gameserver.model.templates.rewards.BonusType;
import com.aionemu.gameserver.model.templates.rewards.CraftItem;
import com.aionemu.gameserver.model.templates.rewards.MedalItem;
import lombok.NoArgsConstructor;

/**
 * 任务/活动加成奖励服务，按加成类型从物品组中随机抽取奖励。
 * Quest/event bonus reward service randomly selecting rewards from item groups by bonus type.
 * @author Rolandas
 */
@Slf4j
@NoArgsConstructor
public class BonusService {

    private static volatile ObjectProvider<BonusService> instanceProvider;
    private static volatile BonusService resolvedInstance;
	private ItemGroupsData itemGroups = DataManager.ITEM_GROUPS_DATA;

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 *
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean / when no provider or bean is available
	 */
	public static BonusService getInstance() {
		BonusService resolved = resolvedInstance;
		if (resolved != null) {
			return resolved;
		}
		ObjectProvider<BonusService> provider = instanceProvider;
		resolved = provider == null ? null : provider.getIfAvailable();
		if (resolved == null) {
			throw new IllegalStateException("BonusService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		resolvedInstance = resolved;
		return resolved;
	}

	/**
	 * 注入 Spring 的实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<BonusService> instanceProvider) {
		BonusService.instanceProvider = instanceProvider;
		resolvedInstance = null;
	}

	/**
	 * 使用指定物品组数据获取（或覆盖）服务实例。
	 * Obtain (or override) the service instance with the given item-group data.
	 * @param itemGroups 物品组数据 / Item groups data
	 * @return 服务实例 / Service instance
	 */
	public static BonusService getInstance(ItemGroupsData itemGroups) {
		BonusService service = getInstance();
		service.itemGroups = itemGroups;
		return service;
	}

    /**
	 * 按加成类型返回对应物品组数组。
	 * Return the bonus item groups for the given bonus type.
	 * @param type 加成类型 / Bonus type
	 * @return 物品组数组，可能为 null / Item group array, may be null
	 */
	public BonusItemGroup[] getGroupsByType(BonusType type) {
		return switch (type) {
			case BOSS -> itemGroups.getBossGroups();
			case ENCHANT -> itemGroups.getEnchantGroups();
			case FOOD -> itemGroups.getFoodGroups();
			case GATHER -> ArrayUtils.addAll(itemGroups.getOreGroups(), itemGroups.getGatherGroups());
			case MANASTONE -> itemGroups.getManastoneGroups();
			case MEDICINE -> itemGroups.getMedicineGroups();
			case TASK -> itemGroups.getCraftGroups();
			case ISLAND, LUNAR, RIFT, VOLATILE_RIFT, REDEEM, MOVIE, MAGICAL, WINTER -> null;
			default -> {
				log.warn(I18n.get("log.42c321e1773d", type));
				yield null;
			}
		};
	}

	/**
	 * 按权重从物品组数组中随机选取一组。
	 * Randomly pick one group from the array weighted by chance.
	 * @param groups 物品组数组 / Item group array
	 * @return 选中的组，可能为 null / Chosen group, may be null
	 */
	public BonusItemGroup getRandomGroup(BonusItemGroup[] groups) {
		float total = 0;
		if (groups == null) {
			return null;
		}
		for (BonusItemGroup gr : groups) {
			total += gr.getChance();
		}
		if (total == 0) {
			return null;
		}

		BonusItemGroup chosenGroup = null;
		if (groups != null) {
			int percent = 100;
			for (BonusItemGroup gr : groups) {
				float chance = getNormalizedChance(gr.getChance(), total);
				if (Rnd.get(0, percent) <= chance) {
					chosenGroup = gr;
					break;
				} else {
					percent -= chance;
				}
			}
		}
		return chosenGroup;
	}

	/**
	 * 将原始概率归一化为百分比。
	 * Normalize a raw chance value into a percentage of the total.
	 * @param chance 原始概率 / Raw chance
	 * @param total 总权重 / Total weight
	 * @return 归一化概率 / Normalized chance
	 */
	float getNormalizedChance(float chance, float total) {
		return chance * 100f / total;
	}

	/**
	 * 按加成类型随机选取一组。
	 * Randomly pick a group for the given bonus type.
	 * @param type 加成类型 / Bonus type
	 * @return 选中的组 / Chosen group
	 */
	public BonusItemGroup getRandomGroup(BonusType type) {
		return getRandomGroup(getGroupsByType(type));
	}

	/**
	 * 根据任务模板计算玩家应得的任务加成物品。
	 * Resolve the quest-bonus item a player should receive for the given quest template.
	 * @param player 玩家 / Player
	 * @param questTemplate 任务模板 / Quest template
	 * @return 任务物品，无加成时返回 null / Quest item, or null if no bonus
	 */
	public QuestItems getQuestBonus(Player player, QuestTemplate questTemplate) {
		List<QuestBonuses> bonuses = questTemplate.getBonus();
		if (bonuses.isEmpty()) {
			return null;
		}
		// 仅一 / Only one
		QuestBonuses bonus = bonuses.getFirst();
		if (bonus.getType() == BonusType.NONE) {
			return null;
		}

		return switch (bonus.getType()) {
			case TASK -> getCraftBonus(player, questTemplate);
			case MANASTONE -> getManastoneBonus(player, bonus);
			case MEDAL -> getMedalBonus(player, questTemplate);
			case ISLAND, LUNAR, RIFT, VOLATILE_RIFT, REDEEM, MOVIE, MAGICAL, WINTER -> null;
			default -> {
				log.warn(I18n.get("log.42c321e1773d", bonus.getType()));
				yield null;
			}
		};
	}

	/**
	 * 解析制作类任务加成奖励。
	 * Resolve craft-task quest bonus rewards.
	 * @param player 玩家 / Player
	 * @param questTemplate 任务模板 / Quest template
	 * @return 任务物品 / Quest item
	 */
	QuestItems getCraftBonus(Player player, QuestTemplate questTemplate) {
		BonusItemGroup[] groups = itemGroups.getCraftGroups();
		CraftGroup group = null;
		ItemRaceEntry[] allRewards = null;

		while (groups != null && groups.length > 0 && group == null) {
			group = (CraftGroup) getRandomGroup(groups);
			if (group == null) {
				break;
			}
			allRewards = group.getRewards(questTemplate.getCombineSkill(), questTemplate.getCombineSkillPoint());
			if (allRewards.length == 0) {
				List<BonusItemGroup> temp = new ArrayList<>();
				Collections.addAll(temp, groups);
				temp.remove(group);
				group = null;
				groups = temp.toArray(new BonusItemGroup[0]);
			}
		}

		if (group == null) { // 可能所有概率都设为 0 / probably all chances set to 0
			return null;
		}
		List<ItemRaceEntry> finalList = new ArrayList<>();

		for (ItemRaceEntry r : allRewards) {
			if (!r.checkRace(player.getCommonData().getRace())) {
				continue;
			}
			finalList.add(r);
		}

		if (finalList.isEmpty()) {
			return null;
		}
		int itemIndex = Rnd.get(finalList.size());
		int itemCount = 1;

		ItemRaceEntry reward = finalList.get(itemIndex);
		if ((reward instanceof CraftItem)) {
			itemCount = Rnd.get(3, 5);
		}
		return new QuestItems(reward.getId(), itemCount);
	}

	/**
	 * 解析勋章类任务加成奖励。
	 * Resolve medal quest bonus rewards.
	 * @param player 玩家 / Player
	 * @param template 任务模板 / Quest template
	 * @return 任务物品 / Quest item
	 */
	QuestItems getMedalBonus(Player player, QuestTemplate template) {
		BonusItemGroup[] groups = itemGroups.getMedalGroups();
		MedalGroup group = (MedalGroup) getRandomGroup(groups);
		int bonusLevel = template.getBonus().getFirst().getLevel();

		MedalItem finalReward = null;

		float total = 0.0F;
		for (MedalItem medal : group.getItems()) {
			if (medal.getLevel() == bonusLevel) {
				total += medal.getChance();
			}
		}
		if (total == 0.0F) {
			return null;
		}
		float rnd = Rnd.get() * total;
		float luck = 0.0F;
		for (MedalItem medal : group.getItems()) {
			if (medal.getLevel() == bonusLevel) {
				luck += medal.getChance();
				if (rnd <= luck) {
					finalReward = medal;
					break;
				}
			}
		}
		return finalReward != null ? new QuestItems(finalReward.getId(), finalReward.getCount()) : null;
	}

	/**
	 * 解析魔石类任务加成奖励。
	 * Resolve manastone quest bonus rewards.
	 * @param player 玩家 / Player
	 * @param bonus 任务加成配置 / Quest bonus config
	 * @return 任务物品 / Quest item
	 */
	QuestItems getManastoneBonus(Player player, QuestBonuses bonus) {
		ManastoneGroup group = (ManastoneGroup) getRandomGroup(BonusType.MANASTONE);
		ItemRaceEntry[] allRewards = group.getRewards();
		List<ItemRaceEntry> finalList = new ArrayList<>();
		for (ItemRaceEntry r : allRewards) {
			ItemTemplate template = DataManager.ITEM_DATA.getItemTemplate(r.getId());
			if (bonus.getLevel() != template.getLevel()) {
				continue;
			}
			finalList.add(r);
		}
		if (finalList.isEmpty()) {
			return null;
		}
		int itemIndex = Rnd.get(finalList.size());
		ItemRaceEntry reward = finalList.get(itemIndex);
		return new QuestItems(reward.getId(), 1);
	}
}
