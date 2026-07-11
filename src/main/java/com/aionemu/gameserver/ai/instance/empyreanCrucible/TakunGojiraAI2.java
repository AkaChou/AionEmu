package com.aionemu.gameserver.ai.instance.empyreanCrucible;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Npc;

/**
 * Empyrean Crucible 副本 NPC AI：Takun Gojira（@AIName "takun_gojira"），继承 AggressiveNpcAI2。
 * Empyrean Crucible instance NPC AI: Takun Gojira (@AIName "takun_gojira"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("takun_gojira")
public class TakunGojiraAI2 extends AggressiveNpcAI2
{
	private Npc counterpart;
	
	@Override
	public void handleSpawned() {
		super.handleSpawned();
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				counterpart = getPosition().getWorldMapInstance().getNpc(getNpcId() == 217596 ? 217597 : 217596);
				if (counterpart != null) {
					getAggroList().addHate(counterpart, 1000000);
				}
			}
		}, 500);
	}
}
