package com.aionemu.gameserver.ai.instance.aturamSkyFortress;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.services.NpcShoutsService;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Aturam Sky Fortress 副本 NPC AI：Weapon H（@AIName "weaponh"），继承 AggressiveNpcAI2。
 * Aturam Sky Fortress instance NPC AI: Weapon H (@AIName "weaponh"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("weaponh")
public class WeaponHAI2 extends AggressiveNpcAI2
{
	private boolean isHome = true;
	private AtomicBoolean isAggred = new AtomicBoolean(false);
	
	@Override
	protected void handleAttack(Creature creature) {
		isHome = false;
		super.handleAttack(creature);
		if (isAggred.compareAndSet(false, true)) {
			// 检测到异常物体。开始清除。 / Abnormal object detected. Elimination beginning.
			GameFeatureServices.npcShoutsService().sendMsg(getOwner(), 1402787, 0);
			getPosition().getWorldMapInstance().getDoors().get(85).setOpen(true);
		}
	}
	
	@Override
	protected void handleBackHome() {
	    isHome = true;
		getPosition().getWorldMapInstance().getDoors().get(85).setOpen(false);
		super.handleBackHome();
	}
	
	@Override
	protected void handleDied() {
		getPosition().getWorldMapInstance().getDoors().get(85).setOpen(true);
		super.handleDied();
	}
}
