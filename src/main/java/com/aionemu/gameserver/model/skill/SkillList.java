package com.aionemu.gameserver.model.skill;

import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * 技能列表。
 * Skill List interface.
 *
 * @author ATracer
 */
public interface SkillList<T extends Creature> {

	/**
	 * 添加技能到列表。 / Add skill to list
	 *
	 * @param creature 生物实体 / Creature
	 * @param skillId 技能 ID / Skill ID
	 * @param skillLevel 技能等级 / Skill level
	 * @return 操作成功时为 true / true if operation was successful
	 */
	boolean addSkill(T creature, int skillId, int skillLevel);

	boolean addLinkedSkill(T creature, int skillId);

	/**
	 * 移除技能列表。 / Remove skill from list
	 *
	 * @param skillId 技能 ID / Skill ID
	 * @return 操作成功时为 true / true if operation was successful
	 */
	boolean removeSkill(int skillId);

	/**
	 * 检查技能是否在列表中。 / Check whether skill is present in list
	 *
	 * @param skillId 技能 ID / Skill ID
	 */
	boolean isSkillPresent(int skillId);

	/**
	 * 获取技能等级。 / Get the skill level
	 *
	 * @param skillId 技能 ID / Skill ID
	 * @return 技能等级 / Skill level
	 */
	int getSkillLevel(int skillId);

	/**
	 * 技能列表大小。 / Size of skill list
	 *
	 * @return 技能数量 / Number of skills
	 */
	int size();
}
