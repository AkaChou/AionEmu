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
 * Fissure Of Oblivion 副本 NPC AI：Orkia Aetheric Field Observatory Square Teleport Stone（@AIName "OAFOSTS"），继承 NpcAI2。
 * Fissure Of Oblivion instance NPC AI: Orkia Aetheric Field Observatory Square Teleport Stone (@AIName "OAFOSTS"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("OAFOSTS")
public class Orkia_Aetheric_Field_Observatory_Square_Teleport_StoneAI2 extends NpcAI2
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
        		OrkiaAethericFieldObservatorySquareTeleportStone();
        	}
        }
    }
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		announceOrkiaAethericField();
	}
	
	private void OrkiaAethericFieldObservatorySquareTeleportStone() {
		AI2Actions.deleteOwner(Orkia_Aetheric_Field_Observatory_Square_Teleport_StoneAI2.this);
		spawn(281446, 855.54144f, 465.55255f, 351.57367f, (byte) 0);
		spawn(834188, 855.54144f, 465.55255f, 351.57367f, (byte) 0, 54);
    }
	
	private void announceOrkiaAethericField() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 你可使用奥尔基亚奥德力场观景广场传送石。 / You can use the Orkia Aetheric Field Observatory Square Teleport Stone.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_IDTransform_SavePoint_01);
				}
			}
		});
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
