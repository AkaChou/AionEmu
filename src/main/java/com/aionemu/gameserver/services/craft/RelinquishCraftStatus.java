package com.aionemu.gameserver.services.craft;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.main.CraftConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.craft.ExpertQuestsList;
import com.aionemu.gameserver.model.craft.MasterQuestsList;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skill.PlayerSkillEntry;
import com.aionemu.gameserver.model.templates.CraftLearnTemplate;
import com.aionemu.gameserver.model.templates.recipe.RecipeTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_COMPLETED_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SKILL_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.services.trade.PricesService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 放弃制作头衔服务：将专家/大师制作技能降级，并清理对应配方与任务。
 * Relinquish craft status service: demotes expert/master craft skills and cleans related recipes and quests.
 */
public class RelinquishCraftStatus {

	private static volatile ObjectProvider<RelinquishCraftStatus> instanceProvider;

	/** 专家最低等级 / Expert minimum skill level */
	private static final int expertMinValue = 399;
	/** 专家最高等级 / Expert maximum skill level */
	private static final int expertMaxValue = 499;
	/** 大师最低等级 / Master minimum skill level */
	private static final int masterMinValue = 499;
	/** 大师最高等级 / Master maximum skill level */
	private static final int masterMaxValue = 549;
	/** 放弃专家费用（基价） / Expert relinquish base price */
	private static final int expertPrice = 120895;
	/** 放弃大师费用（基价） / Master relinquish base price */
	private static final int masterPrice = 3497448;
	/** 基纳不足系统消息 ID / Not-enough-kinah system message id */
	private static final int systemMessageId = 1300388;
	/** 技能变更消息 ID / Skill-change message id */
	private static final int skillMessageId = 1401127;

	/**
	 * 获取服务单例（优先 Spring ObjectProvider）。
	 * Get the service singleton (prefer Spring ObjectProvider when available).
	 *
	 * @return 服务实例 / Service instance
	 */
	public static final RelinquishCraftStatus getInstance() {
		ObjectProvider<RelinquishCraftStatus> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 注入 Spring ObjectProvider，用于容器管理的实例解析。
	 * Inject Spring ObjectProvider for container-managed instance resolution.
	 *
	 * @param provider 实例提供者 / Instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<RelinquishCraftStatus> provider) {
		instanceProvider = provider;
	}

	/**
	 * 放弃专家制作头衔：扣费、降级至专家下限，并清理配方与任务。
	 * Relinquish expert craft status: charge fee, demote to expert min level, clean recipes and quests.
	 *
	 * @param player 玩家 / Player
	 * @param npc 相关 NPC / Related NPC
	 */
	public static void relinquishExpertStatus(Player player, Npc npc) {
		CraftLearnTemplate craftLearnTemplate = CraftSkillUpdateService.npcBySkill.get(npc.getNpcId());
		final int skillId = craftLearnTemplate.getSkillId();
		PlayerSkillEntry skill = player.getSkillList().getSkillEntry(skillId);
		if (!canRelinquishCraftStatus(player, skill, craftLearnTemplate, expertMinValue, expertMaxValue)) {
			return;
		}
		if (!successDecreaseKinah(player, expertPrice)) {
			return;
		}
		skill.setSkillLvl(expertMinValue);
		PacketSendUtility.sendPacket(player, new SM_SKILL_LIST(skill, skillMessageId, false));
		removeRecipesAbove(player, skillId, expertMinValue);
		deleteCraftStatusQuests(skillId, player, true);
	}

	/**
	 * 放弃大师制作头衔：扣费、降级至大师下限，并清理配方与任务。
	 * Relinquish master craft status: charge fee, demote to master min level, clean recipes and quests.
	 *
	 * @param player 玩家 / Player
	 * @param npc 相关 NPC / Related NPC
	 */
	public static void relinquishMasterStatus(Player player, Npc npc) {
		CraftLearnTemplate craftLearnTemplate = CraftSkillUpdateService.npcBySkill.get(npc.getNpcId());
		final int skillId = craftLearnTemplate.getSkillId();
		PlayerSkillEntry skill = player.getSkillList().getSkillEntry(skillId);
		if (!canRelinquishCraftStatus(player, skill, craftLearnTemplate, masterMinValue, masterMaxValue)) {
			return;
		}
		if (!successDecreaseKinah(player, masterPrice)) {
			return;
		}
		skill.setSkillLvl(masterMinValue);
		PacketSendUtility.sendPacket(player, new SM_SKILL_LIST(skill, skillMessageId, false));
		removeRecipesAbove(player, skillId, masterMinValue);
		deleteCraftStatusQuests(skillId, player, false);
	}

	/**
	 * 校验是否可放弃指定区间内的制作头衔。
	 * Validate whether the craft status in the given level range can be relinquished.
	 *
	 * @param player 玩家 / Player
	 * @param skill 技能条目 / Skill entry
	 * @param craftLearnTemplate 制作学习模板 / Craft learn template
	 * @param minValue 最低等级 / Minimum level
	 * @param maxValue 最高等级 / Maximum level
	 *
	 * @return 是否允许放弃 / Whether relinquish is allowed
	 */
	private static boolean canRelinquishCraftStatus(Player player, PlayerSkillEntry skill,
			CraftLearnTemplate craftLearnTemplate, int minValue, int maxValue) {
		if (craftLearnTemplate == null || !craftLearnTemplate.isCraftSkill()) {
			return false;
		}
		if (skill == null || skill.getSkillLevel() < minValue || skill.getSkillLevel() > maxValue) {
			return false;
		}
		return true;
	}

	/**
	 * 按势力价格服务扣减基纳，失败时提示消息。
	 * Decrease kinah using race-aware price service; notify on failure.
	 *
	 * @param player 玩家 / Player
	 * @param basePrice 基础价格 / Base price
	 *
	 * @return 是否扣费成功 / Whether decrease succeeded
	 */
	private static boolean successDecreaseKinah(Player player, int basePrice) {
		if (!player.getInventory().tryDecreaseKinah(PricesService.getPriceForService(basePrice, player.getRace()))) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(systemMessageId));
			return false;
		}
		return true;
	}

	/**
	 * 删除指定技能在给定等级及以上的全部配方。
	 * Remove all recipes for the skill at or above the given skill point level.
	 *
	 * @param player 玩家 / Player
	 * @param skillId 技能 ID / Skill id
	 * @param level 技能点阈值 / Skill-point threshold
	 */
	public static void removeRecipesAbove(Player player, int skillId, int level) {
		for (RecipeTemplate recipe : DataManager.RECIPE_DATA.getRecipeTemplates().values()) {
			if (recipe.getSkillid() != skillId || recipe.getSkillpoint() < level) {
				continue;
			}
			player.getRecipeList().deleteRecipe(player, recipe.getId());
		}
	}

	/**
	 * 删除制作头衔相关任务状态，并刷新任务列表。
	 * Delete craft-status related quest states and refresh the quest list.
	 *
	 * @param skillId 技能 ID / Skill id
	 * @param player 玩家 / Player
	 * @param isExpert 是否同时清理专家任务 / Whether to also clear expert quests
	 */
	public static void deleteCraftStatusQuests(int skillId, Player player, boolean isExpert) {
		for (int questId : MasterQuestsList.getSkillsIds(skillId, player.getRace())) {
			final QuestState qs = player.getQuestStateList().getQuestState(questId);
			if (qs != null) {
				qs.setQuestVar(0);
				qs.setCompleteCount(0);
				qs.setStatus(null);
				qs.setPersistentState(PersistentState.DELETED);
			}
		}
		if (isExpert) {
			for (int questId : ExpertQuestsList.getSkillsIds(skillId, player.getRace())) {
				final QuestState qs = player.getQuestStateList().getQuestState(questId);
				if (qs != null) {
					qs.setQuestVar(0);
					qs.setCompleteCount(0);
					qs.setStatus(null);
					qs.setPersistentState(PersistentState.DELETED);
				}
			}
		}
		PacketSendUtility.sendPacket(player,
				new SM_QUEST_COMPLETED_LIST(player.getQuestStateList().getAllFinishedQuests()));
		player.getController().updateZone();
		player.getController().updateNearbyQuests();
	}

	/**
	 * 当玩家专家/大师数量超出配置上限时，强制降级多余技能。
	 * Force-demote excess expert/master craft skills when over configured limits.
	 *
	 * @param player 玩家 / Player
	 * @param isExpert 是否处理专家（false 为大师，并会递归处理专家） / Process expert (false for master, then recurse to expert)
	 */
	public static void removeExcessCraftStatus(Player player, boolean isExpert) {
		int minValue = isExpert ? expertMinValue : masterMinValue;
		int maxValue = isExpert ? expertMaxValue : masterMaxValue;
		int skillId;
		int skillLevel;
		int maxCraftStatus = isExpert ? CraftConfig.MAX_EXPERT_CRAFTING_SKILLS : CraftConfig.MAX_MASTER_CRAFTING_SKILLS;
		int countCraftStatus;
		for (PlayerSkillEntry skill : player.getSkillList().getBasicSkills()) {
			countCraftStatus = isExpert
					? CraftSkillUpdateService.getTotalMasterCraftingSkills(player)
							+ CraftSkillUpdateService.getTotalExpertCraftingSkills(player)
					: CraftSkillUpdateService.getTotalMasterCraftingSkills(player);
			if (countCraftStatus > maxCraftStatus) {
				skillId = skill.getSkillId();
				skillLevel = skill.getSkillLevel();
				if (CraftSkillUpdateService.isCraftingSkill(skillId) && skillLevel > minValue
						&& skillLevel <= maxValue) {
					skill.setSkillLvl(minValue);
					PacketSendUtility.sendPacket(player, new SM_SKILL_LIST(skill, skillMessageId, false));
					removeRecipesAbove(player, skillId, minValue);
					deleteCraftStatusQuests(skillId, player, isExpert);
				}
				continue;
			}
			break;
		}
		if (!isExpert) {
			removeExcessCraftStatus(player, true);
		}
	}

	/**
	 * 获取专家最低等级。
	 * Get expert minimum skill level.
	 *
	 * @return 专家最低等级 / Expert minimum level
	 */
	public static int getExpertMinValue() {
		return expertMinValue;
	}

	/**
	 * 获取专家最高等级。
	 * Get expert maximum skill level.
	 *
	 * @return 专家最高等级 / Expert maximum level
	 */
	public static int getExpertMaxValue() {
		return expertMaxValue;
	}

	/**
	 * 获取大师最低等级。
	 * Get master minimum skill level.
	 *
	 * @return 大师最低等级 / Master minimum level
	 */
	public static int getMasterMinValue() {
		return masterMinValue;
	}

	/**
	 * 获取大师最高等级。
	 * Get master maximum skill level.
	 *
	 * @return 大师最高等级 / Master maximum level
	 */
	public static int getMasterMaxValue() {
		return masterMaxValue;
	}

	/**
	 * 获取技能变更消息 ID。
	 * Get skill-change message id.
	 *
	 * @return 技能变更消息 ID / Message id
	 */
	public static int getSkillMessageId() {
		return skillMessageId;
	}

	/**
	 * 单例持有者。
	 * Singleton holder.
	 */
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {
		protected static final RelinquishCraftStatus instance = new RelinquishCraftStatus();
	}
}
