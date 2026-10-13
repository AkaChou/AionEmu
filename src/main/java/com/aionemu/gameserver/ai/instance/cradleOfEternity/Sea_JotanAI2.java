package com.aionemu.gameserver.ai.instance.cradleOfEternity;



import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.ThreadPoolManager;

/**
 * Cradle Of Eternity 副本 NPC AI：Sea Jotan（@AIName "Sea_Jotan"），继承 AggressiveNpcAI2。
 * Cradle Of Eternity instance NPC AI: Sea Jotan (@AIName "Sea_Jotan"), extends AggressiveNpcAI2.
 * @author Encom
 */
@AIName("Sea_Jotan")
public class Sea_JotanAI2 extends AggressiveNpcAI2
{
	@Override
	public void think() {
	}

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		ThreadPoolManager.getInstance().schedule(() -> {
			SkillEngine.getInstance().getSkill(getOwner(), 20969, 60, getOwner()).useNoAnimationSkill();
			startLifeTask();
		}, 1000);
	}

	private void startLifeTask() {
		ThreadPoolManager.getInstance().schedule(() -> AI2Actions.deleteOwner(Sea_JotanAI2.this), 10000);
	}

	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
