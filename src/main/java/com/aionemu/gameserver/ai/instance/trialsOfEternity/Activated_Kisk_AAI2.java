package com.aionemu.gameserver.ai.instance.trialsOfEternity;

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
 * Trials Of Eternity 副本 NPC AI：Activated Kisk A（@AIName "IDEternity_03_A_Save_Point"），继承 NpcAI2。
 * Trials Of Eternity instance NPC AI: Activated Kisk A (@AIName "IDEternity_03_A_Save_Point"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("IDEternity_03_A_Save_Point")
public class Activated_Kisk_AAI2 extends NpcAI2
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
        		announceIDEternity03A();
        		IDEternity03ASavePoint();
        	}
        }
    }
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
	}
	
	private void IDEternity03ASavePoint() {
		AI2Actions.deleteOwner(Activated_Kisk_AAI2.this);
		spawn(281446, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading());
		spawn(731731, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading());
    }
	
	private void announceIDEternity03A() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 沉没走廊传送石已激活。 / The Sunken Corridor teleport stone has been activated.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_IDEternity_03_teleporter_1);
				}
			}
		});
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
