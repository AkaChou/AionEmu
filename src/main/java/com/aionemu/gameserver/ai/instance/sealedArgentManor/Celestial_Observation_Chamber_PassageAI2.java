package com.aionemu.gameserver.ai.instance.sealedArgentManor;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * Sealed Argent Manor 副本 NPC AI：Celestial Observation Chamber Passage（@AIName "Celestial_Observation_Chamber_Passage"），继承 NpcAI2。
 * Sealed Argent Manor instance NPC AI: Celestial Observation Chamber Passage (@AIName "Celestial_Observation_Chamber_Passage"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("Celestial_Observation_Chamber_Passage")
public class Celestial_Observation_Chamber_PassageAI2 extends NpcAI2
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
        	if (MathUtil.isIn3dRange(getOwner(), creature, 15)) {
        		observationChamberDoor();
        	}
        }
    }
	
	private void observationChamberDoor() {
		AI2Actions.deleteOwner(Celestial_Observation_Chamber_PassageAI2.this);
		getPosition().getWorldMapInstance().getDoors().get(210).setOpen(true);
    }
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
