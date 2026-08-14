package com.aionemu.gameserver.ai.rvr.asmodianWarshipInvasion;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Npc;

import java.util.List;

/**
 * RvR 相关 NPC AI：Archon Frigate Veteran Protectors Captain（@AIName "DF6_Event_G1_S1_Kn_75_Ah"），继承 AggressiveNpcAI2。
 * RvR-related NPC AI: Archon Frigate Veteran Protectors Captain (@AIName "DF6_Event_G1_S1_Kn_75_Ah"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("DF6_Event_G1_S1_Kn_75_Ah")
public class Archon_Frigate_Veteran_Protectors_CaptainAI2 extends AggressiveNpcAI2
{
	@Override
	public void think() {
	}
	
	@Override
	/**
	 * 死亡后清除护卫队，并召唤特级战斗队长与多名特级突击队长接替。
	 * On death, despawns the protectors and spawns the special grade combat captain with several assault leaders as reinforcements.
	 */
	protected void handleDied() {
		despawnNpc(240664);
		despawnNpc(240665);
		despawnNpc(240666);
		spawn(240667, 1409.8998f, 1369.7438f, 1336.7855f, (byte) 60); //Archon Frigate Special Grade Combat Captain.
		spawn(240668, 1407.2133f, 1371.8616f, 1336.7855f, (byte) 60); //Archon Frigate Special Grade Assault Leader.
		spawn(240668, 1412.3649f, 1367.3982f, 1336.7855f, (byte) 60); //Archon Frigate Special Grade Assault Leader.
		spawn(240668, 1412.2811f, 1372.0088f, 1336.7855f, (byte) 60); //Archon Frigate Special Grade Assault Leader.
		spawn(240668, 1407.1234f, 1367.6641f, 1336.7855f, (byte) 60); //Archon Frigate Special Grade Assault Leader.
		super.handleDied();
		AI2Actions.deleteOwner(this);
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
