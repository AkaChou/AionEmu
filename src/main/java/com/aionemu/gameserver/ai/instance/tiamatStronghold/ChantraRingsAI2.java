package com.aionemu.gameserver.ai.instance.tiamatStronghold;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * Tiamat Stronghold 副本 NPC AI：Chantra Rings（@AIName "chantrarings"），继承 NpcAI2。
 * Tiamat Stronghold instance NPC AI: Chantra Rings (@AIName "chantrarings"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("chantrarings")
public class ChantraRingsAI2 extends NpcAI2
{
    @Override
	public void think() {
	}
	
	@Override
    protected void handleCreatureSee(Creature creature) {
        checkDistance(this, creature);
    }
	
    @Override
    protected void handleCreatureMoved(Creature creature) {
        checkDistance(this, creature);
    }
	
    private void checkDistance(NpcAI2 ai, Creature creature) {
	    int debuff = getOwner().getNpcId() == 283172 ? 20735 : 20734;
        if (creature instanceof Player) {
	    if (getOwner().getNpcId() == 283172 && MathUtil.isIn3dRangeLimited(getOwner(), creature, 10, 18) ||
			    getOwner().getNpcId() == 283171 && MathUtil.isIn3dRangeLimited(getOwner(), creature, 18, 25) ||
				getOwner().getNpcId() == 283171 && MathUtil.isIn3dRangeLimited(getOwner(), creature, 0, 10)) {
		    if (!creature.getEffectController().hasAbnormalEffect(debuff)) {
		        AI2Actions.useSkill(this, debuff);
				}
	    }
        }
	}
	
    @Override
    protected void handleSpawned() {
	    super.handleSpawned();
	    despawn();
    }
	
    private void despawn() {
	    GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
		    @Override
		    public void run() {
			    getOwner().getController().onDelete();
		    }
	    }, 20000);
    }
	
    @Override
	public boolean isMoveSupported() {
		return false;
	}
}
