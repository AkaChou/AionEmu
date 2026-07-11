package com.aionemu.gameserver.ai.instance.evergaleCanyon;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.MathUtil;

import java.util.List;

/**
 * Evergale Canyon 副本 NPC AI：ID Eternity W Tele G To OP L Up（@AIName "IDEternity_W_Tele_G_To_OP_L_Up"），继承 ActionItemNpcAI2。
 * Evergale Canyon instance NPC AI: ID Eternity W Tele G To OP L Up (@AIName "IDEternity_W_Tele_G_To_OP_L_Up"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("IDEternity_W_Tele_G_To_OP_L_Up")
public class IDEternity_W_Tele_G_To_OP_L_UpAI2 extends ActionItemNpcAI2
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
				if (player.getCommonData().getRace() == Race.ASMODIANS) {
					IDEternity_W_Tele_G_To_OP_D_Up();
				}
        	}
        }
    }
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
	}
	
	@Override
	protected void handleUseItemFinish(Player player) {
		switch (getNpcId()) {
		    case 835277: //IDEternity_W_Tele_G_To_OP_L_Up.
				TeleportService2.teleportTo(player, 302350000, 428.87717f, 752.3248f, 336.36725f, (byte) 114);
			break;
        }
	}
	
	private void IDEternity_W_Tele_G_To_OP_D_Up() {
		despawnNpc(835277);
		despawnNpc(835453);
		AI2Actions.deleteOwner(IDEternity_W_Tele_G_To_OP_L_UpAI2.this);
		spawn(835289, 719.26935f, 396.20844f, 305.75839f, (byte) 0, 256);
		spawn(835454, 719.26935f, 396.20844f, 305.75839f, (byte) 0, 319);
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
