package com.aionemu.gameserver.ai.worlds.brusthonin;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.Npc;

import java.util.List;

/**
 * Brusthonin 区域 NPC AI：Steel Rods（@AIName "steel_rods"），继承 NpcAI2。
 * Brusthonin zone NPC AI: Steel Rods (@AIName "steel_rods"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("steel_rods")
public class Steel_RodsAI2 extends NpcAI2
{
	@Override
	protected void handleDied() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
			    despawnNpc(209479); //Captured Griffon's Claw Legionnary.
			}
		}, 3000);
		super.handleDied();
		AI2Actions.deleteOwner(this);
		AI2Actions.scheduleRespawn(this);
	}
	
	private void despawnNpc(int npcId) {
		if (getPosition().getWorldMapInstance().getNpcs(npcId) != null) {
			List<Npc> npcs = getPosition().getWorldMapInstance().getNpcs(npcId);
			for (Npc npc: npcs) {
				npc.getController().onDelete();
			}
		}
	}
}
