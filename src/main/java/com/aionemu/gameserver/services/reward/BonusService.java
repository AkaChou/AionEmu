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

/**
 * 任务/活动加成奖励服务，按加成类型从物品组中随机抽取奖励。
 * Quest/event bonus reward service randomly selecting rewards from item groups by bonus type.
 *
 * @author Rolandas
 */
@Slf4j
public class BonusService {

	private static BonusService instance = new BonusService();
	private static volatile ObjectProvider<BonusService> instanceProvider;
	private ItemGroupsData itemGroups = DataManager.ITEM_GROUPS_DATA;

	/**
	 * 默认构造。
	 * Default constructor.
	 */
	public BonusService() {

	}

	/**
	 * 获取服务单例（优先 Spring ObjectProvider，否则回退本地实例）。
	 * Get the service singleton (prefer Spring ObjectProvider, otherwise local instance).
	 *
	 * Service instance
	 */
	public static BonusService getInstance() {
		ObjectProvider<BonusService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> instance);
		}
		return instance;
	}

	/**
	 * 使用指定物品组数据获取（或覆盖）服务实例。
	 * Obtain (or override) the service instance with the given item-group data.
	 *
	 * @param itemGroups 物品组数据 / Item groups data
	 * Service instance
	 */
	public static BonusService getInstance(ItemGroupsData itemGroups) {
		BonusService service = getInstance();
		service.itemGroups = itemGroups;
		return service;
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Inject the Spring instance provider.
	 *
	 * @param provider 实例提供者 / Instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<BonusService> provider) {
		instanceProvider = provider;
	}

	/**
	 * 按加成类型返回对应物品组数组。
	 * Return the bonus item groups for the given bonus type.
	 *
	 * @param type 加成类型 / Bonus type
	 * @return 物品组数组，可能为 null / Item group array, may be null
	 */
	public BonusItemGroup[] getGroupsByType(BonusType type) {
		switch (type) {
		case BOSS:
			return itemGroups.getBossGroups();
		case ENCHANT:
			return itemGroups.getEnchantGroups();
		case FOOD:
			return itemGroups.getFoodGroups();
		case GATHER:
			return (BonusItemGroup[]) ArrayUtils.addAll(itemGroups.getOreGroups(), itemGroups.getGatherGroups());
		case MANASTONE:
			return itemGroups.getManastoneGroups();
		case MEDICINE:
			return itemGroups.getMedicineGroups();
		case TASK:
			return itemGroups.getCraftGroups();
		case ISLAND:
		case LUNAR:
		case RIFT:
		case VOLATILE_RIFT:
		case REDEEM:
		case MOVIE:
		case MAGICAL:
		case WINTER:
			return null;
		default:
			log.warn(I18n.get("log.42c321e1773d", type));
			return null;
		}
	}

	/**
	 * 按权重从物品组数组中随机选取一组。
	 * Randomly pick one group from the array weighted by chance.
	 *
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
	 *
	 * Raw chance
	 * Total weight
	 * @return 归一化概率 / Normalized chance
	 */
	float getNormalizedChance(float chance, float total) {
		return chance * 100f / total;
	}

	/**
	 * 按加成类型随机选取一组。
	 * Randomly pick a group for the given bonus type.
	 *
	 * @param type 加成类型 / Bonus type
	 * Chosen group
	 */
	public BonusItemGroup getRandomGroup(BonusType type) {
		return getRandomGroup(getGroupsByType(type));
	}

	/**
	 * 根据任务模板计算玩家应得的任务加成物品。
	 * Resolve the quest-bonus item a player should receive for the given quest template.
	 *
	 * 玩家 / Player
	 * Quest template
	 * @return 任务物品，无加成时返回 null / Quest item, or null if no bonus
	 */
	public QuestItems getQuestBonus(Player player, QuestTemplate questTemplate) {
		List<QuestBonuses> bonuses = questTemplate.getBonus();
		if (bonuses.isEmpty()) {
			return null;
		}
		// 仅一 / Only one
		QuestBonuses bonus = bonuses.get(0);
		if (bonus.getType() == BonusType.NONE) {
			return null;
		}

		switch (bonus.getType()) {
		case TASK:
			return getCraftBonus(player, questTemplate);
		case MANASTONE:
			return getManastoneBonus(player, bonus);
		case MEDAL:
			return getMedalBonus(player, questTemplate);
		case ISLAND:
		case LUNAR:
		case RIFT:
		case VOLATILE_RIFT:
		case REDEEM:
		case MOVIE:
		case MAGICAL:
		case WINTER:
			return null;
		default:
			log.warn(I18n.get("log.42c321e1773d", bonus.getType()));
			return null;
		}
	}

	/**
	 * 解析制作类任务加成奖励。
	 * Resolve craft-task quest bonus rewards.
	 *
	 * 玩家 / Player
	 * Quest template
	 * Quest item
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
				List<BonusItemGroup> temp = new ArrayList<BonusItemGroup>();
				Collections.addAll(temp, groups);
				temp.remove(group);
				group = null;
				groups = temp.toArray(new BonusItemGroup[0]);
			}
		}

		if (group == null) { // probably all chances set to 0
			return null;
		}
		List<ItemRaceEntry> finalList = new ArrayList<ItemRaceEntry>();

		for (int i = 0; i < allRewards.length; i++) {
			ItemRaceEntry r = allRewards[i];
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
	 *
	 * 玩家 / Player
	 * Quest template
	 * Quest item
	 */
	QuestItems getMedalBonus(Player player, QuestTemplate template) {
		BonusItemGroup[] groups = itemGroups.getMedalGroups();
		MedalGroup group = (MedalGroup) getRandomGroup(groups);
		int bonusLevel = template.getBonus().get(0).getLevel();

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
	 *
	 * 玩家 / Player
	 * @param bonus  任务加成配置 / Quest bonus config
	 * Quest item
	 */
	QuestItems getManastoneBonus(Player player, QuestBonuses bonus) {
		ManastoneGroup group = (ManastoneGroup) getRandomGroup(BonusType.MANASTONE);
		ItemRaceEntry[] allRewards = group.getRewards();
		List<ItemRaceEntry> finalList = new ArrayList<ItemRaceEntry>();
		for (int i = 0; i < allRewards.length; i++) {
			ItemRaceEntry r = allRewards[i];
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
