package com.aionemu.gameserver.ai.instance.seizedDanuarSanctuary;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.ChatType;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldPosition;

import java.util.List;

/**
 * Seized Danuar Sanctuary 副本 NPC AI：Steel Rose Cannon（@AIName "steelrosecannon2"），继承 ActionItemNpcAI2。
 * Seized Danuar Sanctuary instance NPC AI: Steel Rose Cannon (@AIName "steelrosecannon2"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("steelrosecannon2")
public class SteelRoseCannonAI2 extends ActionItemNpcAI2
{
	@Override
	protected void handleUseItemFinish(Player player) {
		if (!player.getInventory().decreaseByItemId(186000254, 1)) {
			PacketSendUtility.broadcastPacket(player, new SM_MESSAGE(player,
			"You must have <Seal Breaking Magic Cannonball>", ChatType.BRIGHT_YELLOW_CENTER), true);
			return;
		}
		WorldPosition worldPosition = player.getPosition();
		if (worldPosition.isInstanceMap()) {
			// 被占领的达努阿尔圣所 4.8 / Seized Danuar Sanctuary 4.8
			if (worldPosition.getMapId() == 301140000) {
				//某处沉重的门已打开。 / A heavy door has opened somewhere.
				PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_IDLDF5_Under_02_Canon, 5000);
				GameEngineServices.skillEngine().getSkill(getOwner(), 21126, 60, getOwner()).useNoAnimationSkill(); //Destroy Seal.
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    @Override
					public void run() {
					    despawnNpc(233142); //Unyielding Boulder.
					}
				}, 5000);
			}
			// 达努阿尔圣所 4.8 / Danuar Sanctuary 4.8
			else if (worldPosition.getMapId() == 301380000) {
				//某处沉重的门已打开。 / A heavy door has opened somewhere.
				PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_IDLDF5_Under_02_Canon, 5000);
				GameEngineServices.skillEngine().getSkill(getOwner(), 21126, 60, getOwner()).useNoAnimationSkill(); //Destroy Seal.
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    @Override
					public void run() {
					    despawnNpc(233142); //Unyielding Boulder.
					}
				}, 5000);
			}
		}
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
	
	private void despawnNpc(int npcId) {
		if (getPosition().getWorldMapInstance().getNpcs(npcId) != null) {
			List<Npc> npcs = getPosition().getWorldMapInstance().getNpcs(npcId);
			for (Npc npc: npcs) {
				AI2Actions.killSilently(this, npc);
			}
		}
	}
}
