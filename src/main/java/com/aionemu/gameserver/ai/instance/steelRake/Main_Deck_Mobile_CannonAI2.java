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
		// 消耗拉吉马克的燧石激活主甲板移动加农；5 秒后清除封印相关 NPC。 / Consume Largimark's Flint to activate the main deck mobile cannon; despawn the seal NPCs after 5 seconds.
		if (!player.getInventory().decreaseByItemId(185000052, 1)) {
			announceMainDeckMobileCannon();
			return;
		}
		WorldPosition worldPosition = player.getPosition();
		if (worldPosition.isInstanceMap()) {
			if (worldPosition.getMapId() == 300100000) {
				GameEngineServices.skillEngine().getSkill(getOwner(), 21126, 60, getOwner()).useNoAnimationSkill(); // 破坏封印 / Destroy Seal.
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
	
	/**
	 * 向副本内所有玩家广播缺少燧石的提示。
	 * Broadcast the missing-flint message to all players in the instance.
	 */
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
	
	/**
	 * 静默击杀副本内指定 ID 的全部 NPC。
	 * Silently kill all NPCs of the given ID in the instance.
	 *
	 * @param npcId 要清除的 NPC ID / NPC ID to remove
	 */
	private void despawnNpc(int npcId) {
		if (getPosition().getWorldMapInstance().getNpcs(npcId) != null) {
			List<Npc> npcs = getPosition().getWorldMapInstance().getNpcs(npcId);
			for (Npc npc: npcs) {
				AI2Actions.killSilently(this, npc);
			}
		}
	}
}
