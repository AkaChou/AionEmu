package com.aionemu.gameserver.ai.rvr.asmodianWarshipInvasion;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Npc;

import java.util.List;

/**
 * RvR 相关 NPC AI：Archon Frigate Special Grade Combat Captain（@AIName "DF6_Event_G1_S2_Fi_75_Al"），继承 AggressiveNpcAI2。
 * RvR-related NPC AI: Archon Frigate Special Grade Combat Captain (@AIName "DF6_Event_G1_S2_Fi_75_Al"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("DF6_Event_G1_S2_Fi_75_Al")
public class Archon_Frigate_Special_Grade_Combat_CaptainAI2 extends AggressiveNpcAI2
{
	@Override
	public void think() {
	}
	
	@Override
	protected void handleDied() {
		switch (Rnd.get(1, 3)) {
			case 1:
				spawn(240669, 1409.9818f, 1369.7706f, 1336.7855f, (byte) 60); //Suminid.
			break;
			case 2:
				spawn(240670, 1409.9818f, 1369.7706f, 1336.7855f, (byte) 60); //Taina.
			break;
			case 3:
				spawn(240671, 1409.9818f, 1369.7706f, 1336.7855f, (byte) 60); //Vassad.
			break;
		}
		super.handleDied();
		despawnNpc(240668); //Archon Frigate Special Grade Assault Leader.
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
