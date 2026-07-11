package com.aionemu.gameserver.ai.instance.cradleOfEternity;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * Cradle Of Eternity 副本 NPC AI：Walking Path Bind Point（@AIName "Walking_Path_Bind_Point"），继承 NpcAI2。
 * Cradle Of Eternity instance NPC AI: Walking Path Bind Point (@AIName "Walking_Path_Bind_Point"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("Walking_Path_Bind_Point")
public class Walking_Path_Bind_PointAI2 extends NpcAI2
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
        		WalkingPathBindPoint();
        	}
        }
    }
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		announceWalkingPath();
	}
	
	private void WalkingPathBindPoint() {
		AI2Actions.deleteOwner(Walking_Path_Bind_PointAI2.this);
		spawn(281446, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading());
		spawn(806037, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Geodesis <To The Garden Temple>
    }
	
	private void announceWalkingPath() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 步行道的绑定点装置已激活。 / The Walking Path's bind point device was activated.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_IDEternity_02_SYSTEM_MSG_11);
				}
			}
		});
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
