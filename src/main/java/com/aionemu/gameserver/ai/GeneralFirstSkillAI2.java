package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * 通用且优先技能的 NPC AI：选择攻击意图时优先技能。
 * General NPC AI that prefers a skill when choosing attack intention.
 *
 * @author Encom
 */
@AIName("general_first_skill")
public class GeneralFirstSkillAI2 extends GeneralNpcAI2
{
	/**
	 * 处理归位完成事件。
	 * Handle back-home.
	 */
	@Override
	protected void handleBackHome() {
		super.handleBackHome();
		if (getSkillList().getUseInSpawnedSkill() != null) {
			int skillId = getSkillList().getUseInSpawnedSkill().getSkillId();
			int skillLevel = getSkillList().getSkillLevel(skillId);
			GameEngineServices.skillEngine().getSkill(getOwner(), skillId, skillLevel, getOwner()).useSkill();
		}
	}
	
	/**
	 * 处理生成完成事件。
	 * Handle post-spawn.
	 */
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		if (getSkillList().getUseInSpawnedSkill() != null) {
			int skillId = getSkillList().getUseInSpawnedSkill().getSkillId();
			int skillLevel = getSkillList().getSkillLevel(skillId);
			GameEngineServices.skillEngine().getSkill(getOwner(), skillId, skillLevel, getOwner()).useSkill();
		}
	}
	
	/**
	 * 处理重生完成事件。
	 * Handle post-respawn.
	 */
	@Override
	protected void handleRespawned() {
		super.handleRespawned();
		if (getSkillList().getUseInSpawnedSkill() != null) {
			int skillId = getSkillList().getUseInSpawnedSkill().getSkillId();
			int skillLevel = getSkillList().getSkillLevel(skillId);
			GameEngineServices.skillEngine().getSkill(getOwner(), skillId, skillLevel, getOwner()).useSkill();
		}
	}
}
