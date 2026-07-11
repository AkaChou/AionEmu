package com.aionemu.gameserver.ai.instance.fissureOfOblivion;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * Fissure Of Oblivion 副本 NPC AI：Fissure Of Oblivion Bind Point（@AIName "Fissure_Of_Oblivion_Bind_Point"），继承 NpcAI2。
 * Fissure Of Oblivion instance NPC AI: Fissure Of Oblivion Bind Point (@AIName "Fissure_Of_Oblivion_Bind_Point"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("Fissure_Of_Oblivion_Bind_Point")
public class Fissure_Of_Oblivion_Bind_PointAI2 extends NpcAI2
{
	@Override
    protected void handleCreatureSee(Creature creature) {
        checkDistance(this, creature);
    }
	
    @Override
    protected void handleCreatureMoved(Creature creature) {
        checkDistance(this, creature);
    }
	
	private void checkDistance(NpcAI2 ai, Creature creature) {
        if (creature instanceof Player && !creature.getLifeStats().isAlreadyDead()) {
        	if (MathUtil.isIn3dRange(getOwner(), creature, 10)) {
        		FissureOfOblivionBindPoint();
        	}
        }
    }
	
	private void FissureOfOblivionBindPoint() {
		AI2Actions.deleteOwner(Fissure_Of_Oblivion_Bind_PointAI2.this);
		spawn(281446, 404.32239f, 513.28625f, 342.31073f, (byte) 0);
		spawn(730845, 404.32239f, 513.28625f, 342.31073f, (byte) 0, 56);
    }
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
