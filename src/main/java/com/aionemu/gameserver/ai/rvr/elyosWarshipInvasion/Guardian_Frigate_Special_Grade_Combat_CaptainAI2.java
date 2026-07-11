package com.aionemu.gameserver.ai.rvr.elyosWarshipInvasion;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Npc;

import java.util.List;

/**
 * RvR 相关 NPC AI：Guardian Frigate Special Grade Combat Captain（@AIName "LF6_Event_G1_S2_Fi_75_Al"），继承 AggressiveNpcAI2。
 * RvR-related NPC AI: Guardian Frigate Special Grade Combat Captain (@AIName "LF6_Event_G1_S2_Fi_75_Al"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("LF6_Event_G1_S2_Fi_75_Al")
public class Guardian_Frigate_Special_Grade_Combat_CaptainAI2 extends AggressiveNpcAI2
{
	@Override
	public void think() {
	}
	
	@Override
	protected void handleDied() {
		switch (Rnd.get(1, 3)) {
			case 1:
				spawn(240762, 1391.9735f, 1615.5792f, 1010.55457f, (byte) 25); //Luluran.
			break;
			case 2:
				spawn(240763, 1391.9735f, 1615.5792f, 1010.55457f, (byte) 25); //Nanabel.
			break;
			case 3:
				spawn(240764, 1391.9735f, 1615.5792f, 1010.55457f, (byte) 25); //Mandos.
			break;
		}
		super.handleDied();
		despawnNpc(240761); //Guardian Frigate Special Grade Assault Leader.
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
