package com.aionemu.gameserver.ai.instance.secretMunitionsFactory;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.WorldPosition;

import java.util.List;

/**
 * Secret Munitions Factory 副本 NPC AI：Azurespark Apparatus（@AIName "azurespark_apparatus"），继承 ActionItemNpcAI2。
 * Secret Munitions Factory instance NPC AI: Azurespark Apparatus (@AIName "azurespark_apparatus"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("azurespark_apparatus")
public class Azurespark_ApparatusAI2 extends ActionItemNpcAI2
{
	@Override
	protected void handleUseItemFinish(Player player) {
		WorldPosition worldPosition = player.getPosition();
		if (worldPosition.isInstanceMap()) {
			if (worldPosition.getMapId() == 301640000) { //Secret Munitions Factory.
				WorldMapInstance worldMapInstance = worldPosition.getWorldMapInstance();
				killNpc(worldMapInstance.getNpcs(243661)); //Azure Living Bomb.
			}
		}
	}
	
	private void killNpc(List<Npc> npcs) {
		for (Npc npc: npcs) {
			AI2Actions.killSilently(this, npc);
		}
	}
}
