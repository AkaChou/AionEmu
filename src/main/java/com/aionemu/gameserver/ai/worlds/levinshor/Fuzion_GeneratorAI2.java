package com.aionemu.gameserver.ai.worlds.levinshor;



import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.ThreadPoolManager;

/**
 * Levinshor 区域 NPC AI：Fuzion Generator（@AIName "fuzion_generator"），继承 NpcAI2。
 * Levinshor zone NPC AI: Fuzion Generator (@AIName "fuzion_generator"), extends NpcAI2.
 * @author Encom
 */
@AIName("fuzion_generator")
public class Fuzion_GeneratorAI2 extends NpcAI2
{
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		ThreadPoolManager.getInstance().schedule(() -> {
			SkillEngine.getInstance().getSkill(getOwner(), 22776, 1, getOwner()).useNoAnimationSkill();
			SkillEngine.getInstance().getSkill(getOwner(), 22781, 1, getOwner()).useNoAnimationSkill();
			SkillEngine.getInstance().getSkill(getOwner(), 22783, 1, getOwner()).useNoAnimationSkill();
		}, 1000);
	}

	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
