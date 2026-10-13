package com.aionemu.gameserver.ai.instance.bastionOfSouls;



import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.ThreadPoolManager;

/**
 * Bastion Of Souls 副本 NPC AI：Earthen Fire（@AIName "IDAb1_Ere_Smigol_Area"），继承 AggressiveNpcAI2。
 * Bastion Of Souls instance NPC AI: Earthen Fire (@AIName "IDAb1_Ere_Smigol_Area"), extends AggressiveNpcAI2.
 * @author Encom
 */
@AIName("IDAb1_Ere_Smigol_Area")
public class Earthen_FireAI2 extends AggressiveNpcAI2
{
	@Override
	public void think() {
	}

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		ThreadPoolManager.getInstance().schedule(() -> {
			SkillEngine.getInstance().getSkill(getOwner(), 17655, 60, getOwner()).useNoAnimationSkill(); //IDAb1_Ere_Smigol_AreaFire.
			startLifeTask();
		}, 1000);
	}

	private void startLifeTask() {
		ThreadPoolManager.getInstance().schedule(() -> AI2Actions.deleteOwner(Earthen_FireAI2.this), 10000);
	}

	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
