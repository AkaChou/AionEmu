package com.aionemu.gameserver.ai.instance.fissureOfOblivion;

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
 * Fissure Of Oblivion 副本 NPC AI：Orkia Spire Teleport Stone（@AIName "Orkia_Spire_Teleport_Stone"），继承 NpcAI2。
 * Fissure Of Oblivion instance NPC AI: Orkia Spire Teleport Stone (@AIName "Orkia_Spire_Teleport_Stone"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("Orkia_Spire_Teleport_Stone")
public class Orkia_Spire_Teleport_StoneAI2 extends NpcAI2
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
	 * 玩家进入 10 米范围内时激活传送石：删除自身并生成目标传送点。
	 * Activates the teleport stone when a player comes within 10m: deletes itself and spawns the destination teleport points.
	 */
	private void checkDistance(NpcAI2 ai, Creature creature) {
        if (creature instanceof Player && !creature.getLifeStats().isAlreadyDead()) {
        	if (MathUtil.isIn3dRange(getOwner(), creature, 10)) {
        		OrkiaSpireTeleportStone();
        	}
        }
    }
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		announceOrkiaSpire();
	}
	
	private void OrkiaSpireTeleportStone() {
		AI2Actions.deleteOwner(Orkia_Spire_Teleport_StoneAI2.this);
		spawn(281446, 522.48053f, 573.51971f, 321.80389f, (byte) 0);
		spawn(834190, 522.48053f, 573.51971f, 321.80389f, (byte) 0, 55);
    }
	
	private void announceOrkiaSpire() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 你可使用奥尔基亚尖塔传送石。 / You can use the Orkia Spire Teleport Stone.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_IDTransform_SavePoint_03);
				}
			}
		});
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
