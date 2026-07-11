package com.aionemu.gameserver.ai.instance.ophidanWarpath;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;

/**
 * Ophidan Warpath 副本 NPC AI：Mechanical Weapon Test Part（@AIName "Mechanical_Weapon_Test_Part"），继承 ActionItemNpcAI2。
 * Ophidan Warpath instance NPC AI: Mechanical Weapon Test Part (@AIName "Mechanical_Weapon_Test_Part"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("Mechanical_Weapon_Test_Part")
public class Mechanical_Weapon_Test_PartAI2 extends ActionItemNpcAI2
{
    private boolean isRewarded;
	
    @Override
    protected void handleDialogStart(Player player) {
        InstanceReward<?> instance = getPosition().getWorldMapInstance().getInstanceHandler().getInstanceReward();
        if (instance != null && !instance.isStartProgress()) {
            return;
        }
        super.handleDialogStart(player);
    }
	
    @Override
    protected void handleUseItemFinish(Player player) {
        if (!isRewarded) {
            isRewarded = true;
            AI2Actions.handleUseItemFinish(this, player);
            AI2Actions.deleteOwner(this);
        }
    }
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		startLifeTask();
	}
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(Mechanical_Weapon_Test_PartAI2.this);
			}
		}, 59000);
	}
}
