package com.aionemu.gameserver.ai.worlds.inggison;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;

/**
 * Inggison 区域 NPC AI：Clone Of Barrier（@AIName "omega_clone"），继承 AggressiveNpcAI2。
 * Inggison zone NPC AI: Clone Of Barrier (@AIName "omega_clone"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("omega_clone")
public class Clone_Of_BarrierAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		for (VisibleObject object: getKnownList().getKnownObjectsSnapshot()) {
			if (object instanceof Npc) {
				Npc npc = (Npc) object;
				if (npc.getNpcId() == 216516) { // Boss：Omega / Omega.
					npc.getEffectController().removeEffect(18671); // 魔法护盾 / Magic Ward.
					break;
				}
			}
		}
		super.handleDied();
	}
}
