package com.aionemu.gameserver.ai.instance.steelRake;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldPosition;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.List;

/**
 * Steel Rake 副本 NPC AI：Main Deck Mobile Cannon（@AIName "main_deck_mobile_cannon"），继承 ActionItemNpcAI2。
 * Steel Rake instance NPC AI: Main Deck Mobile Cannon (@AIName "main_deck_mobile_cannon"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("main_deck_mobile_cannon")
public class Main_Deck_Mobile_CannonAI2 extends ActionItemNpcAI2
{
	@Override
	protected void handleUseItemFinish(Player player) {
		if (!player.getInventory().decreaseByItemId(185000052, 1)) {
			announceMainDeckMobileCannon();
			return;
		}
		WorldPosition worldPosition = player.getPosition();
		if (worldPosition.isInstanceMap()) {
			if (worldPosition.getMapId() == 300100000) {
				GameEngineServices.skillEngine().getSkill(getOwner(), 21126, 60, getOwner()).useNoAnimationSkill(); //Destroy Seal.
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					@Override
					public void run() {
						despawnNpc(214968);
						despawnNpc(215402);
						despawnNpc(215403);
						despawnNpc(215404);
						despawnNpc(215405);
					}
				}, 5000);
			}
		}
	}
	
	private void announceMainDeckMobileCannon() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 我需要拉吉马克的燧石。 / I'll need Largimark's Flint.
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(false, 1111302, player.getObjectId(), 2));
				}
			}
		});
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
