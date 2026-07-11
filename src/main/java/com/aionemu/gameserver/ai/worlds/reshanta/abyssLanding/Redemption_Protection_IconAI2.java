package com.aionemu.gameserver.ai.worlds.reshanta.abyssLanding;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Reshanta 区域 NPC AI：Redemption Protection Icon（@AIName "redemption_protection_icon"），继承 AggressiveNpcAI2。
 * Reshanta zone NPC AI: Redemption Protection Icon (@AIName "redemption_protection_icon"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("redemption_protection_icon")
public class Redemption_Protection_IconAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		GameEngineServices.skillEngine().getSkill(getOwner(), 22781, 1, getOwner()).useNoAnimationSkill(); //Guardian Sanctuary Icon.
		GameEngineServices.skillEngine().getSkill(getOwner(), 22783, 1, getOwner()).useNoAnimationSkill(); //Guardian Sanctuary Field.
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
	
	@Override
	protected void handleDied() {
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
