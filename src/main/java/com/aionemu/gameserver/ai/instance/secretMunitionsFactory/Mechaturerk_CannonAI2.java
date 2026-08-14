package com.aionemu.gameserver.ai.instance.secretMunitionsFactory;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldPosition;

import java.util.List;

/**
 * Secret Munitions Factory 副本 NPC AI：Mechaturerk Cannon（@AIName "mechaturerk_cannon"），继承 ActionItemNpcAI2。
 * Secret Munitions Factory instance NPC AI: Mechaturerk Cannon (@AIName "mechaturerk_cannon"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("mechaturerk_cannon")
public class Mechaturerk_CannonAI2 extends ActionItemNpcAI2
{
	@Override
	/**
	 * 使用加农炮后：提示玩家沉重的门已打开，释放破坏封印技能，并延时清除两扇门。
	 * After using the cannon: notifies that a heavy door has opened, casts the Destroy Seal skill, and despawns both doors after a delay.
	 */
	protected void handleUseItemFinish(Player player) {
		WorldPosition worldPosition = player.getPosition();
		if (worldPosition.isInstanceMap()) {
			if (worldPosition.getMapId() == 301640000) { //Secret Munitions Factory.
				//某处沉重的门已打开。 / A heavy door has opened somewhere.
				PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_IDLDF5_Under_02_Canon, 5000);
				GameEngineServices.skillEngine().getSkill(getOwner(), 21126, 60, getOwner()).useNoAnimationSkill(); //破坏封印。 / Destroy Seal.
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					@Override
					public void run() {
						despawnNpc(833869);
						despawnNpc(833835);
					}
				}, 5000);
			}
		}
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
