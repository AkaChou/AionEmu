package com.aionemu.gameserver.ai.instance.cradleOfEternity;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.List;

/**
 * Cradle Of Eternity 副本 NPC AI：Elyos Elite Combat Mage（@AIName "Elyos_Elite_Combat_Mage"），继承 NpcAI2。
 * Cradle Of Eternity instance NPC AI: Elyos Elite Combat Mage (@AIName "Elyos_Elite_Combat_Mage"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("Elyos_Elite_Combat_Mage")
public class Elyos_Elite_Combat_MageAI2 extends NpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		if (player.isArchDaeva()) {
		    PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
		} else {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 27));
        }
	}
	
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000) {
			switch (getNpcId()) {
				case 220575: // 天族精英战斗法师 / Elyos Elite Combat Mage.
					announce1StDefense();
				break;
				case 220577: // 天族精英战斗法师 / Elyos Elite Combat Mage.
					announce2NdDefense();
				break;
				case 220579: // 天族精英战斗法师 / Elyos Elite Combat Mage.
					announce3RdDefense();
				break;
				case 220581: // 天族精英战斗法师 / Elyos Elite Combat Mage.
					announce4ThDefense();
			    break;
			}
		}
		AI2Actions.deleteOwner(this);
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		return true;
	}
	
	private void announce1StDefense() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 护盾倒下，第 1 防线被突破。 / As the shields fell, the 1st Defense Line was breached and overrun.
				PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_IDEternity_02_SYSTEM_MSG_01, 0);
				// 敌人来了。全部消灭。 / The enemies are coming. Kill them all.
				PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_IDEternity_02_SYSTEM_MSG_34, 5000);
			}
		});
	}
	private void announce2NdDefense() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 护盾倒下，第 2 防线被突破。 / As the shields fell, the 2nd Defense Line was breached and overrun.
				PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_IDEternity_02_SYSTEM_MSG_02, 0);
				// 敌人来了。全部消灭。 / The enemies are coming. Kill them all.
				PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_IDEternity_02_SYSTEM_MSG_34, 5000);
			}
		});
	}
	private void announce3RdDefense() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 护盾倒下，第 3 防线被突破。 / As the shields fell, the 3rd Defense Line was breached and overrun.
				PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_IDEternity_02_SYSTEM_MSG_03, 0);
				// 敌人来了。全部消灭。 / The enemies are coming. Kill them all.
				PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_IDEternity_02_SYSTEM_MSG_34, 5000);
			}
		});
	}
	private void announce4ThDefense() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 护盾倒下，第 4 防线被突破。 / As the shields fell, the 4th Defense Line was breached and overrun.
				PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_IDEternity_02_SYSTEM_MSG_04, 0);
				// 敌人来了。全部消灭。 / The enemies are coming. Kill them all.
				PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_IDEternity_02_SYSTEM_MSG_34, 5000);
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
}
