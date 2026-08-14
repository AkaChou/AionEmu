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
 * Trials Of Eternity 副本 NPC AI：Activated Kisk C（@AIName "IDEternity_03_C_Save_Point"），继承 NpcAI2。
 * Trials Of Eternity instance NPC AI: Activated Kisk C (@AIName "IDEternity_03_C_Save_Point"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("IDEternity_03_C_Save_Point")
public class Activated_Kisk_CAI2 extends NpcAI2
{
	@Override
    protected void handleCreatureSee(Creature creature) {
        checkDistance(this, creature);
    }
	
	@Override
	protected void handleCreatureMoved(Creature creature) {
		checkDistance(this, creature);
	}
	
	/**
	 * 当玩家进入 15 米范围内时激活该传送点并广播系统消息。
	 * Activates this save point and broadcasts a system message when a player comes within 15 meters.
	 */
	private void checkDistance(NpcAI2 ai, Creature creature) {
		if (creature instanceof Player && !creature.getLifeStats().isAlreadyDead()) {
        	if (MathUtil.isIn3dRange(getOwner(), creature, 15)) {
        		announceIDEternity03C();
        		IDEternity03CSavePoint();
        	}
        }
    }

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
	}

	/**
	 * 删除未激活的传送石，在原地刷新激活版传送石。
	 * Deletes the inactive teleport stone and spawns the activated version at the same spot.
	 */
	private void IDEternity03CSavePoint() {
		AI2Actions.deleteOwner(Activated_Kisk_CAI2.this);
		spawn(281446, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading());
		spawn(731733, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading());
    }
	
	private void announceIDEternity03C() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 走廊 B 传送石已激活。 / The Corridor B teleport stone has been activated.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_IDEternity_03_teleporter_3);
				}
			}
		});
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
