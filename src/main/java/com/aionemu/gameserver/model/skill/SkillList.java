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
	 * 添加技能列表。 / Add skill to list
	 *
	 * @return true if operation was successful
	 */
	boolean addSkill(T creature, int skillId, int skillLevel);

	boolean addLinkedSkill(T creature, int skillId);

	/**
	 * 移除技能列表。 / Remove skill from list
	 *
	 * @return true if operation was successful
	 */
	boolean removeSkill(int skillId);

	/**
	 * 检查技能是否在列表中。 / Check whether skill is present in list
	 */
	boolean isSkillPresent(int skillId);

	int getSkillLevel(int skillId);

	/**
	 * Size of skill list
	 */
	int size();
}
