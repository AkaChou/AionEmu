package com.aionemu.gameserver.ai.instance.evergaleCanyon;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.List;

/**
 * Evergale Canyon 副本 NPC AI：ID Eternity W Tele I To OP Up（@AIName "IDEternity_W_Tele_I_To_OP_Up"），继承 NpcAI2。
 * Evergale Canyon instance NPC AI: ID Eternity W Tele I To OP Up (@AIName "IDEternity_W_Tele_I_To_OP_Up"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("IDEternity_W_Tele_I_To_OP_Up")
public class IDEternity_W_Tele_I_To_OP_UpAI2 extends NpcAI2
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
			final Player player = (Player) creature;
        	if (MathUtil.isIn3dRange(getOwner(), creature, 10)) {
        		if (player.getCommonData().getRace() == Race.ELYOS) {
					IDEternity_W_Tele_I_To_OP_L_Up();
					announceTele05E();
				} else if (player.getCommonData().getRace() == Race.ASMODIANS) {
					IDEternity_W_Tele_I_To_OP_D_Up();
					announceTele06A();
				}
        	}
        }
    }
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		announceTele11();
	}
	
	private void IDEternity_W_Tele_I_To_OP_L_Up() {
		despawnNpc(835292);
		despawnNpc(835454);
		AI2Actions.deleteOwner(IDEternity_W_Tele_I_To_OP_UpAI2.this);
		spawn(835280, 1035.4257f, 1065.4717f, 350.2265f, (byte) 0, 56);
		spawn(835453, 1035.4257f, 1065.4717f, 350.2265f, (byte) 0, 300);
    }
	private void IDEternity_W_Tele_I_To_OP_D_Up() {
		despawnNpc(835280);
		despawnNpc(835453);
		AI2Actions.deleteOwner(IDEternity_W_Tele_I_To_OP_UpAI2.this);
		spawn(835292, 1035.4257f, 1065.4717f, 350.2265f, (byte) 0, 56);
		spawn(835454, 1035.4257f, 1065.4717f, 350.2265f, (byte) 0, 298);
    }
	
	private void announceTele11() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					//I? ??  ? .
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_IDEternity_War_tele_11);
				}
			}
		});
	}
	private void announceTele05E() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					//I? ??  ? ? ?  .
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_IDEternity_War_tele_05);
				}
			}
		});
	}
	private void announceTele06A() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					//I? ??  ? ? ?  .
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_IDEternity_War_tele_06);
				}
			}
		});
	}
	
	private void despawnNpc(int npcId) {
		if (getPosition().getWorldMapInstance().getNpcs(npcId) != null) {
			List<Npc> npcs = getPosition().getWorldMapInstance().getNpcs(npcId);
			for (Npc npc: npcs) {
				npc.getController().onDelete();
			}
		}
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
