package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameStaticDataServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.HTMLService;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;
import java.util.LinkedHashMap;
import java.util.Map;

import java.util.Set;

/**
 * 诺克萨纳训练营副本事件处理器。
 * Instance event handler for Nochsana Training Camp.
 *
 * @author Encom
 * @author MATTY (ADev.Team)
 */

@InstanceID(300030000)
public class NochsanaTrainingCampInstance extends GeneralInstanceHandler
{
	/**
	@Override
    public void onEnterInstance(Player player) {
		HTMLService.showHTML(player, GameStaticDataServices.htmlCache().getHTML("instances/nochsanaTrainingCamp.xhtml"));
    }
	 * */

	/**
	 * NPC 掉落表注册时处理。
	 * Handle NPC drop-table registration.
	 * 
	 * @param npc NPC / npc
	 */
	@Override
    public void onDropRegistered(Npc npc) {
        Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		int index = dropItems.size() + 1;
        switch (npcId) {
			case 256693: // 诺克萨纳将军。 / Nochsana General.
			    for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053787, 1)); //烙印之石支援包。 / Stigma Support Bundle.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188051138, 1)); //[Event] Nochsana Camp Treasure Chest.
					}
				}
			break;
        }
    }
	
//	@Override
//	public void onDie(Npc npc) {
//		Player player = npc.getAggroList().getMostPlayerDamage();
//		switch (npc.getObjectTemplate().getTemplateId()) {
//			case 256689: // 诺克萨纳卫兵。 / Nochsana Guard.
//				despawnNpc(npc);
//				sendMsg("<Nochsana General> appear"); // Появился Лорд Насана
//				spawn(256693, 331.097f, 269.36f, 384.553f, (byte) 25); // 诺克萨纳将军。 / Nochsana General.
//			break;
//			case 256693: // 诺克萨纳将军。 / Nochsana General.
//			    sendMsg("<Nochsana Abyss Gate> is now open");
//				spawn(700438, 466.7858f, 706.5129f, 346.2541f, (byte) 0, 14);
//			break;
//		}
//	}
	
	/**
	 * 玩家对 NPC 使用物品完成时处理。
	 * Handle item-use finish on an NPC.
	 *
	 * @param player 玩家 / player
	 * @param npc NPC / npc
	 */
	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		switch(npc.getNpcId()) {
			case 700437: //Nochsana Artifact.
				//sendMsg("You win effect <Shield Of Compassion>");
				GameEngineServices.skillEngine().getSkill(npc, 276, 10, player).useNoAnimationSkill();
			break;
		}
	}

	/**
	 * 将军死亡后在其当前位置重建出口门，便于击杀后直接离开。
	 * Recreate the exit gate at the General's current death position for a direct post-kill exit.
	 *
	 * @param npc 死亡的 NPC / the dead NPC
	 */
	@Override
	public void onDie(Npc npc) {
		if (npc.getNpcId() == 256693) { // 诺克萨纳将军。 / Nochsana General.
			spawn(700438, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading());
			sendMsg("Nochsana Abyss Gate has appeared.");
		}
	}
	
	private void sendMsg(final String str) {
		instance.doOnAllPlayers(new Visitor<Player>() {
			/**
			 * 处理 visit。
			 * Handle visit.
			 *
			 * @param player 玩家 / player
			 */
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendWhiteMessageOnCenter(player, str);
			}
		});
	}
	
	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}
}
