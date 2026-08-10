package com.aionemu.gameserver.questEngine.definition;

import java.util.LinkedHashMap;
import java.util.Map;

/** Explicit legacy XML path to canonical metadata path mapping. */
public final class QuestMetadataFieldMapping {
	private static final Map<String, String> MAPPING;

	static {
		Map<String, String> fields = new LinkedHashMap<>();
		fields.put("id", "QuestDefinition.id");
		fields.put("name", "QuestMetadata.name");
		fields.put("nameId", "QuestMetadata.displayNameId");
		fields.put("minlevel_permitted", "QuestMetadata.minLevel");
		fields.put("maxlevel_permitted", "QuestMetadata.maxLevel");
		fields.put("rank", "QuestMetadata.rank");
		fields.put("max_repeat_count", "QuestMetadata.repeatPolicy.maxRepeatCount");
		fields.put("reward_repeat_count", "QuestMetadata.repeatPolicy.rewardRepeatCount");
		fields.put("max_count_limited_quest", "QuestMetadata.maxCountLimitedQuest");
		fields.put("count_recover_limited_quest", "QuestMetadata.countRecoverLimitedQuest");
		fields.put("cannot_share", "QuestMetadata.cannotShare");
		fields.put("cannot_giveup", "QuestMetadata.cannotGiveup");
		fields.put("bounty_reward", "QuestMetadata.bountyReward");
		fields.put("use_class_reward", "QuestMetadata.useClassReward");
		fields.put("race_permitted", "QuestMetadata.permittedRaces");
		fields.put("combineskill", "QuestMetadata.combineSkill");
		fields.put("combine_skillpoint", "QuestMetadata.combineSkillPoint");
		fields.put("timer", "QuestMetadata.timer");
		fields.put("category", "QuestMetadata.category");
		fields.put("repeat_cycle", "QuestMetadata.repeatCycles");
		fields.put("npcfaction_id", "QuestMetadata.npcFactionId");
		fields.put("mentor_type", "QuestMetadata.mentorType");
		fields.put("mentor", "QuestMetadata.mentorType");
		fields.put("target_type", "QuestMetadata.targetType");
		fields.put("quest_cooltime", "QuestMetadata.repeatPolicy.cooldownSeconds");
		fields.put("titleId", "QuestMetadata.titleId");
		fields.put("collect_items", "QuestMetadata.itemRequirements");
		fields.put("inventory_items", "QuestMetadata.inventoryItems");
		fields.put("rewards", "QuestMetadata.rewardGroups");
		fields.put("bonus", "QuestMetadata.bonuses");
		fields.put("extended_rewards", "QuestMetadata.extendedRewardGroups");
		fields.put("quest_drop", "QuestMetadata.drops");
		fields.put("quest_kill", "QuestMetadata.kills");
		fields.put("start_conditions", "QuestMetadata.startConditionGroups");
		fields.put("class_permitted", "QuestMetadata.permittedClasses");
		fields.put("gender_permitted", "QuestMetadata.permittedGender");
		fields.put("quest_work_items", "QuestMetadata.questWorkItems");
		fields.put("fighter_selectable_reward", "QuestMetadata.classRewards.FIGHTER");
		fields.put("knight_selectable_reward", "QuestMetadata.classRewards.KNIGHT");
		fields.put("ranger_selectable_reward", "QuestMetadata.classRewards.RANGER");
		fields.put("assassin_selectable_reward", "QuestMetadata.classRewards.ASSASSIN");
		fields.put("wizard_selectable_reward", "QuestMetadata.classRewards.WIZARD");
		fields.put("elementalist_selectable_reward", "QuestMetadata.classRewards.ELEMENTALIST");
		fields.put("priest_selectable_reward", "QuestMetadata.classRewards.PRIEST");
		fields.put("chanter_selectable_reward", "QuestMetadata.classRewards.CHANTER");
		fields.put("gunslinger_selectable_reward", "QuestMetadata.classRewards.GUNSLINGER");
		fields.put("songweaver_selectable_reward", "QuestMetadata.classRewards.SONGWEAVER");
		fields.put("aethertech_selectable_reward", "QuestMetadata.classRewards.AETHERTECH");
		MAPPING = Map.copyOf(fields);
	}

	private QuestMetadataFieldMapping() {
	}

	public static Map<String, String> mapping() {
		return MAPPING;
	}
}
