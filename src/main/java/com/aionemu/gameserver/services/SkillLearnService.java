package com.aionemu.gameserver.services;

import com.aionemu.gameserver.configs.main.MembershipConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skill.PlayerSkillEntry;
import com.aionemu.gameserver.model.skill.PlayerSkillList;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SKILL_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SKILL_REMOVE;
import com.aionemu.gameserver.skillengine.model.SkillLearnTemplate;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 技能学习服务，处理升级学技、补技、技能书与移除。
 * Skill learn service handling level-up skills, missing skills, skill books, and removal.
 */
public class SkillLearnService {
	/**
	 * 玩家升级时学习当前等级新技能（含 10 级制作技能迁移）。
	 * Learns new skills for the player's current level (including level-10 craft skill migration).
	 *
	 * @param player 玩家 / player
	 */
	public static void addNewSkills(Player player) {
		int level = player.getCommonData().getLevel();
		PlayerClass playerClass = player.getCommonData().getPlayerClass();
		Race playerRace = player.getRace();
		PacketSendUtility.sendPacket(player, new SM_SKILL_LIST(player, player.getSkillList().getBasicSkills()));

		if (level == 10 && player.getSkillList().getSkillEntry(30001) != null) {
			int skillLevel = player.getSkillList().getSkillLevel(30001);
			removeSkill(player, 30001);
			PacketSendUtility.sendPacket(player, new SM_SKILL_LIST(player, player.getSkillList().getBasicSkills()));
			// 为何在数据包之后添加？ / Why adding after the packet ?
			player.getSkillList().addSkill(player, 30002, skillLevel);
		}

		addSkills(player, level, playerClass, playerRace);
	}

	/**
	 * 补全玩家当前等级及转职前应掌握的全部技能。
	 * Fills in all skills the player should know for current level and starting class.
	 *
	 * @param player 玩家 / player
	 */
	public static void addMissingSkills(Player player) {
		int level = player.getCommonData().getLevel();
		PlayerClass playerClass = player.getCommonData().getPlayerClass();
		Race playerRace = player.getRace();
		for (int i = 0; i <= level; i++) {
			addSkills(player, i, playerClass, playerRace);
		}
		if (!playerClass.isStartingClass()) {
			PlayerClass startinClass = PlayerClass.getStartingClassFor(playerClass);
			for (int i = 1; i < 10; i++) {
				addSkills(player, i, startinClass, playerRace);
			}
			PacketSendUtility.sendPacket(player, new SM_SKILL_LIST(player, player.getSkillList().getBasicSkills()));
			for (PlayerSkillEntry stigmaSkill : player.getSkillList().getStigmaSkills()) {
				PacketSendUtility.sendPacket(player, new SM_SKILL_LIST(player, stigmaSkill));
			}
		}
	}

	/**
	 * 4P 场景下补全玩家缺失技能（逻辑与 {@link #addMissingSkills} 相同）。
	 * Fills missing skills in the 4P path (same logic as {@link #addMissingSkills}).
	 *
	 * @param player 玩家 / player
	 */
	public static void addMissingSkills4P(Player player) {
		int level = player.getCommonData().getLevel();
		PlayerClass playerClass = player.getCommonData().getPlayerClass();
		Race playerRace = player.getRace();
		for (int i = 0; i <= level; i++) {
			addSkills(player, i, playerClass, playerRace);
		}
		if (!playerClass.isStartingClass()) {
			PlayerClass startinClass = PlayerClass.getStartingClassFor(playerClass);
			for (int i = 1; i < 10; i++) {
				addSkills(player, i, startinClass, playerRace);
			}
			PacketSendUtility.sendPacket(player, new SM_SKILL_LIST(player, player.getSkillList().getBasicSkills()));
			for (PlayerSkillEntry stigmaSkill : player.getSkillList().getStigmaSkills()) {
				PacketSendUtility.sendPacket(player, new SM_SKILL_LIST(player, stigmaSkill));
			}
		}
	}

	/**
	 * 按职业/等级/种族模板向玩家添加可学技能。
	 * Adds learnable skills to the player from class/level/race templates.
	 *
	 * 玩家 / player
	 * level
	 * player class
	 * player race
	 */
	public static void addSkills(Player player, int level, PlayerClass playerClass, Race playerRace) {
		SkillLearnTemplate[] skillTemplates = DataManager.SKILL_TREE_DATA.getTemplatesFor(playerClass, level, playerRace);
		PlayerSkillList playerSkillList = player.getSkillList();
		for (SkillLearnTemplate template : skillTemplates) {
			if (!checkLearnIsPossible(player, playerSkillList, template)) {
				continue;
			}
			if (template.isStigma()) {
				playerSkillList.addStigmaSkill(player, template.getSkillId(), template.getSkillLevel());
			}
			if (playerSkillList.isCraftSkill(template.getSkillId()) && player.getSkillList().isSkillPresent(template.getSkillId())) {
				continue;
			} else {
				playerSkillList.addSkill(player, template.getSkillId(), template.getSkillLevel());
			}
		}
	}

	/**
	 * 判断模板技能是否允许学习（含自动学习与会员自动印记）。
	 * Checks whether the template skill may be learned (auto-learn and membership stigma).
	 *
	 * 玩家 / player
	 * skill list
	 * learn template
	 * whether learnable
	 */
	private static boolean checkLearnIsPossible(Player player, PlayerSkillList playerSkillList, SkillLearnTemplate template) {
		if (playerSkillList.isSkillPresent(template.getSkillId())) {
			return true;
		}
		if (!template.isStigma()) {
			return true;
		}
		if ((player.havePermission(MembershipConfig.STIGMA_AUTOLEARN) && template.isStigma())) {
			return true;
		}
		if (template.isAutoLearn()) {
			return true;
		}
		return false;
	}

	/**
	 * 通过技能书学习技能至玩家当前可达最高等级。
	 * Learns a skill from a skill book up to the max level available for the player.
	 *
	 * 玩家 / player
	 * skill id
	 */
	public static void learnSkillBook(Player player, int skillId) {
		SkillLearnTemplate[] skillTemplates = null;
		int maxLevel = 0;
		SkillTemplate passiveSkill = DataManager.SKILL_DATA.getSkillTemplate(skillId);
		for (int i = 1; i <= player.getLevel(); i++) {
			skillTemplates = DataManager.SKILL_TREE_DATA.getTemplatesFor(player.getPlayerClass(), i, player.getRace());
			for (SkillLearnTemplate skill : skillTemplates) {
				if (skillId == skill.getSkillId()) {
					if (skill.getSkillLevel() > maxLevel) {
						maxLevel = skill.getSkillLevel();
					}
				}
			}
		}
		player.getSkillList().addSkill(player, skillId, maxLevel);
		if (passiveSkill.isPassive()) {
			player.getController().updatePassiveStats();
		}
	}

	/**
	 * 移除玩家技能并同步客户端（含异常效果清理）。
	 * Removes a player skill and syncs the client (including abnormal effect cleanup).
	 *
	 * 玩家 / player
	 * skill id
	 */
	public static void removeSkill(Player player, int skillId) {
		if (player.getSkillList().isSkillPresent(skillId)) {
			Integer skillLevel = player.getSkillList().getSkillLevel(skillId);
			if (skillLevel == 0) {
				skillLevel = 1;
			}
			if (player.getEffectController().hasAbnormalEffect(skillId)) {
				player.getEffectController().removeEffect(skillId);
			}
			PacketSendUtility.sendPacket(player, new SM_SKILL_REMOVE(skillId, skillLevel, player.getSkillList().getSkillEntry(skillId).isStigma(), false));
			player.getSkillList().removeSkill(skillId);
		}
	}

	/**
	 * 移除玩家链接技能并清空 linkedSkill 标记。
	 * Removes a linked skill and clears the linkedSkill flag.
	 *
	 * 玩家 / player
	 * skill id
	 */
	public static void removeLinkedSkill(Player player, int skillId) {
		if (player.getSkillList().isSkillPresent(skillId)) {
			Integer skillLevel = player.getSkillList().getSkillLevel(skillId);
			if (skillLevel == 0) {
				skillLevel = 1;
			}
			if (player.getEffectController().hasAbnormalEffect(skillId)) {
				player.getEffectController().removeEffect(skillId);
			}
			PacketSendUtility.sendPacket(player, new SM_SKILL_REMOVE(skillId, skillLevel, false, player.getSkillList().getSkillEntry(skillId).isLinked()));
			player.getSkillList().removeSkill(skillId);
			player.setLinkedSkill(0);
		}
	}

	/**
	 * 按玩家等级与目标技能等级计算实际可学技能等级。
	 * Computes the actual skill level learnable for the player level and wanted level.
	 *
	 * skill id
	 * player level
	 * @param wantedSkillLevel 期望技能等级 / wanted skill level
	 * @return 实际技能等级 / actual skill level
	 */
	public static int getSkillLearnLevel(int skillId, int playerLevel, int wantedSkillLevel) {
		SkillLearnTemplate[] skillTemplates = DataManager.SKILL_TREE_DATA.getTemplatesForSkill(skillId);
		int learnFinishes = 0;
		int maxLevel = 0;
		for (SkillLearnTemplate template : skillTemplates) {
			if (maxLevel < template.getSkillLevel()) {
				maxLevel = template.getSkillLevel();
			}
		}
		if (maxLevel == 0) {
			return wantedSkillLevel;
		}
		learnFinishes = playerLevel + maxLevel;
		if (learnFinishes > DataManager.PLAYER_EXPERIENCE_TABLE.getMaxLevel()) {
			learnFinishes = DataManager.PLAYER_EXPERIENCE_TABLE.getMaxLevel();
		}
		return Math.max(wantedSkillLevel, Math.min(playerLevel - (learnFinishes - maxLevel) + 1, maxLevel));
	}

	/**
	 * 获取学习指定技能等级所需的最低玩家等级。
	 * Returns the minimum player level required to learn the wanted skill level.
	 *
	 * skill id
	 * player level
	 * @param wantedSkillLevel 期望技能等级 / wanted skill level
	 * @return 最低玩家等级 / minimum player level
	 */
	public static int getSkillMinLevel(int skillId, int playerLevel, int wantedSkillLevel) {
		SkillLearnTemplate[] skillTemplates = DataManager.SKILL_TREE_DATA.getTemplatesForSkill(skillId);
		SkillLearnTemplate foundTemplate = null;
		for (SkillLearnTemplate template : skillTemplates) {
			if (template.getSkillLevel() <= wantedSkillLevel && template.getMinLevel() <= playerLevel) {
				foundTemplate = template;
			}
		}
		if (foundTemplate == null) {
			return playerLevel;
		}
		return foundTemplate.getMinLevel();
	}
}
