package com.aionemu.gameserver.ai.worlds.levinshor;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Levinshor 区域 NPC AI：Fuzion Generator（@AIName "fuzion_generator"），继承 NpcAI2。
 * Levinshor zone NPC AI: Fuzion Generator (@AIName "fuzion_generator"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("fuzion_generator")
public class Fuzion_GeneratorAI2 extends NpcAI2
{
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				GameEngineServices.skillEngine().getSkill(getOwner(), 22776, 1, getOwner()).useNoAnimationSkill();
				GameEngineServices.skillEngine().getSkill(getOwner(), 22781, 1, getOwner()).useNoAnimationSkill();
				GameEngineServices.skillEngine().getSkill(getOwner(), 22783, 1, getOwner()).useNoAnimationSkill();
			}
		}, 1000);
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
