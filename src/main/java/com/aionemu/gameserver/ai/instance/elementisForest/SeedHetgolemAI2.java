package com.aionemu.gameserver.ai.instance.elementisForest;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.actions.NpcActions;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.world.WorldPosition;

/**
 * Elementis Forest 副本 NPC AI：Seed Hetgolem（@AIName "seed_hetgolem"），继承 AggressiveNpcAI2。
 * Elementis Forest instance NPC AI: Seed Hetgolem (@AIName "seed_hetgolem"), extends AggressiveNpcAI2.
 *
 * @author xTz
 */
@AIName("seed_hetgolem")
public class SeedHetgolemAI2 extends AggressiveNpcAI2 {

	@Override
	public void handleDied() {
		WorldPosition p = getPosition();
		if (p != null && p.getWorldMapInstance() != null) {
			spawn(282441, p.getX(), p.getY(), p.getZ(), p.getHeading());
			Npc npc = (Npc)spawn(282465, p.getX(), p.getY(), p.getZ(), p.getHeading());
			NpcActions.delete(npc);
		}
		super.handleDied();
		AI2Actions.deleteOwner(this);
		
	}
}
