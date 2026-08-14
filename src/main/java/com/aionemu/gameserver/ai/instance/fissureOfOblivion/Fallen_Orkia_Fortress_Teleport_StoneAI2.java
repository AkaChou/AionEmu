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
 * Fissure Of Oblivion 副本 NPC AI：Fallen Orkia Fortress Teleport Stone（@AIName "Fallen_Orkia_Fortress_Teleport_Stone"），继承 NpcAI2。
 * Fissure Of Oblivion instance NPC AI: Fallen Orkia Fortress Teleport Stone (@AIName "Fallen_Orkia_Fortress_Teleport_Stone"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("Fallen_Orkia_Fortress_Teleport_Stone")
public class Fallen_Orkia_Fortress_Teleport_StoneAI2 extends NpcAI2
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
        		FallenOrkiaFortressTeleportStone();
        	}
        }
    }
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		announceFallenOrkia();
	}
	
	private void FallenOrkiaFortressTeleportStone() {
		AI2Actions.deleteOwner(Fallen_Orkia_Fortress_Teleport_StoneAI2.this);
		spawn(281446, 594.41882f, 564.05542f, 352.56454f, (byte) 0);
		spawn(834189, 594.41882f, 564.05542f, 352.56454f, (byte) 0, 58);
    }
	
	private void announceFallenOrkia() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 你可使用陨落奥尔基亚要塞传送石。 / You can use the Fallen Orkia Fortress Teleport Stone.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_IDTransform_SavePoint_02);
				}
			}
		});
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
