package com.aionemu.gameserver.ai.worlds.kaldor;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;

/**
 * Kaldor 区域 NPC AI：Anoha Sword Effect（@AIName "anoha_sword_effect"），继承 NpcAI2。
 * Kaldor zone NPC AI: Anoha Sword Effect (@AIName "anoha_sword_effect"), extends NpcAI2.
 * @author Encom
 */
@AIName("anoha_sword_effect")
public class Anoha_Sword_EffectAI2 extends NpcAI2
{
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		GameThreadPoolServices.threadPoolManager().schedule(() -> startLifeTask(), 1000);
	}

	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(() -> AI2Actions.deleteOwner(Anoha_Sword_EffectAI2.this), 1790000);
	}

	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
